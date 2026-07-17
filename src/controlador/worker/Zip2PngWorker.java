package controlador.worker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import modelo.renderer.ImageEntry;
import modelo.renderer.StlEntry;
import modelo.renderer.Triangle;
import servicios.renderer.AwtModelRenderer;
import servicios.renderer.StlParser;
import servicios.renderer.ZipExtractor;
import servicios.renderer.Zip2PngScanner.RenderCandidate;
import vista.dialogos.TaskProgressDialog;

public class Zip2PngWorker extends SwingWorker<Void, String> {

    public record SourceInfo(Path pngPath, RenderCandidate candidate, StlEntry stlEntry) {}

    private static final Logger logger = LoggerFactory.getLogger(Zip2PngWorker.class);

    private final List<RenderCandidate> candidates;
    private final Path outputDir;
    private final TaskProgressDialog dialog;
    private final Runnable onDone;
    private final AwtModelRenderer renderer;
    private final List<Path> generatedFiles;
    private final List<SourceInfo> sourceInfos;

    public Zip2PngWorker(List<RenderCandidate> candidates, Path outputDir,
            TaskProgressDialog dialog, Runnable onDone) {
        this.candidates = candidates;
        this.outputDir = outputDir;
        this.dialog = dialog;
        this.onDone = onDone;
        this.renderer = new AwtModelRenderer();
        this.generatedFiles = new ArrayList<>();
        this.sourceInfos = new ArrayList<>();
        if (this.dialog != null) {
            this.dialog.setWorkerAsociado(this);
        }
    }

    public List<Path> getGeneratedFiles() {
        return generatedFiles;
    }

    public List<SourceInfo> getSourceInfos() {
        return sourceInfos;
    }

    @Override
    protected Void doInBackground() throws Exception {
        Files.createDirectories(outputDir);

        int total = candidates.size();
        for (int i = 0; i < total; i++) {
            if (isCancelled()) return null;

            RenderCandidate candidate = candidates.get(i);
            String msg = "Procesando " + candidate.nombreBase + " (" + (i + 1) + "/" + total + ")";
            publish(msg);
            logger.debug(msg);

            try {
                Path stlPath;
                Path tempDir = null;
                StlEntry largest = null;

                if (candidate.esComprimido) {
                    publish("Extrayendo " + candidate.nombreBase + "...");
                    tempDir = ZipExtractor.extractToTemp(candidate.path);
                    List<StlEntry> allStls = buscarTodosSTL(tempDir);
                    if (allStls.isEmpty()) {
                        publish("No se encontró STL en " + candidate.nombreBase);
                        if (tempDir != null) ZipExtractor.deleteDir(tempDir);
                        continue;
                    }
                    largest = allStls.stream()
                            .max(Comparator.comparingLong(StlEntry::sizeBytes))
                            .orElse(allStls.get(0));
                    stlPath = tempDir.resolve(largest.filename());
                } else {
                    stlPath = candidate.path;
                }

                publish("Renderizando " + stlPath.getFileName().toString());
                List<Triangle> triangles = StlParser.parse(stlPath.toFile());
                java.awt.image.BufferedImage img = renderer.renderizar(triangles);

                String pngName = candidate.nombreBase + ".png";
                Path pngPath = outputDir.resolve(pngName);
                javax.imageio.ImageIO.write(img, "PNG", pngPath.toFile());
                generatedFiles.add(pngPath);
                if (candidate.esComprimido && largest != null) {
                    sourceInfos.add(new SourceInfo(pngPath, candidate, largest));
                } else {
                    sourceInfos.add(new SourceInfo(pngPath, candidate,
                            new StlEntry(stlPath.getFileName().toString(), stlPath.toFile().length())));
                }
                publish("Generado " + pngName);

                if (tempDir != null) {
                    ZipExtractor.deleteDir(tempDir);
                }

                // Extraer imágenes embebidas si las hay
                if (candidate.tieneImagenesDentro()) {
                    Path imgBaseDir = outputDir.resolve("imagenes").resolve(candidate.nombreBase);
                    Files.createDirectories(imgBaseDir);
                    for (ImageEntry imgEntry : candidate.imagenesInternas) {
                        if (isCancelled()) return null;
                        publish("Extrayendo imagen " + imgEntry.filename());
                        try {
                            ZipExtractor.extractSingleFile(candidate.path, imgEntry.filename(), imgBaseDir);
                        } catch (Exception ex) {
                            logger.warn("No se pudo extraer imagen {} de {}: {}",
                                    imgEntry.filename(), candidate.nombreBase, ex.getMessage());
                        }
                    }
                    publish("Imágenes extraídas de " + candidate.nombreBase);
                }
            } catch (Exception e) {
                logger.error("Error procesando {}: {}", candidate.nombreBase, e.getMessage());
                publish("Error: " + candidate.nombreBase + " - " + e.getMessage());
            }

            setProgress((i + 1) * 100 / total);
        }
        return null;
    }

    private List<StlEntry> buscarTodosSTL(Path dir) throws IOException {
        List<StlEntry> result = new ArrayList<>();
        try (var walk = java.nio.file.Files.walk(dir)) {
            walk.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".stl"))
                    .filter(Files::isRegularFile)
                    .forEach(p -> {
                        String relative = dir.relativize(p).toString().replace('\\', '/');
                        try {
                            result.add(new StlEntry(relative, Files.size(p)));
                        } catch (IOException e) {
                            result.add(new StlEntry(relative, 0));
                        }
                    });
        }
        return result;
    }

    @Override
    protected void process(List<String> chunks) {
        if (dialog != null) {
            dialog.updateStatusText(chunks.get(chunks.size() - 1));
        }
    }

    @Override
    protected void done() {
        SwingUtilities.invokeLater(() -> {
            if (dialog != null) {
                dialog.closeDialog();
            }
            if (!isCancelled() && onDone != null) {
                onDone.run();
            }
        });
    }

} // --- Fin de la clase Zip2PngWorker ---
