package vista.components;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import modelo.datos.Tag;

public class TagSearchEngine {

    public static final int MAX_RESULTS = 30;

    public enum InputMode {
        PLAIN, WILDCARD, PATH, DRILL_DOWN
    }

    private List<Tag> allTags = new ArrayList<>();
    private Map<Long, Tag> tagById = new HashMap<>();
    private Map<Long, List<Tag>> childrenByParentId = new HashMap<>();
    private Map<Long, String> fullPathCache = new HashMap<>();
    private List<String> allPaths = new ArrayList<>();
    private Map<String, List<Tag>> nameToTags = new HashMap<>();

    public void loadTags(List<Tag> tags) {
        this.allTags = new ArrayList<>(tags);
        this.tagById.clear();
        this.childrenByParentId.clear();
        this.fullPathCache.clear();
        this.allPaths.clear();
        this.nameToTags.clear();

        for (Tag t : this.allTags) {
            this.tagById.put(t.getId(), t);
            Long pid = t.getParentId();
            childrenByParentId.computeIfAbsent(pid, k -> new ArrayList<>()).add(t);
            nameToTags.computeIfAbsent(t.getNombre().toLowerCase(), k -> new ArrayList<>()).add(t);
        }

        List<Tag> roots = childrenByParentId.getOrDefault(null, Collections.emptyList());
        Deque<Tag> queue = new ArrayDeque<>();
        for (Tag root : roots) {
            fullPathCache.put(root.getId(), root.getNombre());
            queue.add(root);
        }
        while (!queue.isEmpty()) {
            Tag current = queue.poll();
            String currentPath = fullPathCache.get(current.getId());
            List<Tag> children = childrenByParentId.get(current.getId());
            if (children != null) {
                for (Tag child : children) {
                    fullPathCache.put(child.getId(), currentPath + "." + child.getNombre());
                    queue.add(child);
                }
            }
        }

        this.allPaths = fullPathCache.values().stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public boolean isEmpty() {
        return allTags.isEmpty();
    }

    public static InputMode parseInputMode(String text) {
        boolean hasLeadingDot = text.startsWith(".");
        boolean hasTrailingDot = text.endsWith(".") && text.length() > 1;

        if (hasTrailingDot) {
            return InputMode.DRILL_DOWN;
        }
        if (hasLeadingDot) {
            return InputMode.WILDCARD;
        }
        if (text.contains(".")) {
            return InputMode.PATH;
        }
        return InputMode.PLAIN;
    }

    public List<PathResult> search(String text, InputMode mode) {
        return switch (mode) {
            case WILDCARD -> searchWildcard(text);
            case PATH -> searchPath(text);
            case DRILL_DOWN -> searchDrillDown(text);
            case PLAIN -> searchPlain(text);
        };
    }

    private List<PathResult> searchPlain(String prefix) {
        String lower = prefix.toLowerCase();
        return allTags.stream()
                .filter(t -> t.getNombre().toLowerCase().startsWith(lower))
                .limit(MAX_RESULTS)
                .map(t -> pathResult(t))
                .collect(Collectors.toList());
    }

    private List<PathResult> searchWildcard(String text) {
        String search = text.substring(1).toLowerCase();
        if (search.isEmpty()) {
            return rootTagPaths();
        }
        return allPaths.stream()
                .filter(p -> p.toLowerCase().contains(search))
                .limit(MAX_RESULTS)
                .map(p -> new PathResult(p, p))
                .collect(Collectors.toList());
    }

    private List<PathResult> searchPath(String text) {
        int lastDot = text.lastIndexOf(".");
        if (lastDot < 0) return Collections.emptyList();

        String parentPath = text.substring(0, lastDot);
        String childPrefix = text.substring(lastDot + 1);

        Tag parent = resolveTagByPath(parentPath);
        if (parent == null) return Collections.emptyList();

        List<Tag> children = childrenByParentId.get(parent.getId());
        if (children == null) return Collections.emptyList();

        String lowerPrefix = childPrefix.toLowerCase();
        return children.stream()
                .filter(t -> t.getNombre().toLowerCase().startsWith(lowerPrefix))
                .limit(MAX_RESULTS)
                .map(t -> pathResult(t))
                .collect(Collectors.toList());
    }

    private List<PathResult> searchDrillDown(String text) {
        if (text.startsWith(".")) {
            // Search drill-down: .x. → find all tags named X (any level), show their children
            String nameToFind = text.substring(1, text.length() - 1);
            if (nameToFind.isEmpty()) return Collections.emptyList();

            String lowerName = nameToFind.toLowerCase();
            List<PathResult> results = new ArrayList<>();
            for (Tag t : allTags) {
                if (t.getNombre().toLowerCase().equals(lowerName)) {
                    List<Tag> children = childrenByParentId.get(t.getId());
                    if (children != null) {
                        for (Tag child : children) {
                            results.add(pathResult(child));
                            if (results.size() >= MAX_RESULTS) break;
                        }
                    }
                    if (results.size() >= MAX_RESULTS) break;
                }
            }
            return results;
        } else {
            // Imperative drill-down: x. → show children of the resolved path
            String path = text.substring(0, text.length() - 1);
            if (path.isEmpty()) return Collections.emptyList();

            Tag parent = resolveTagByPath(path);
            if (parent == null) return Collections.emptyList();

            List<Tag> children = childrenByParentId.get(parent.getId());
            if (children == null || children.isEmpty()) return Collections.emptyList();

            return children.stream()
                    .limit(MAX_RESULTS)
                    .map(t -> pathResult(t))
                    .collect(Collectors.toList());
        }
    }

    private List<PathResult> rootTagPaths() {
        List<Tag> roots = childrenByParentId.get(null);
        if (roots == null || roots.isEmpty()) return Collections.emptyList();
        return roots.stream()
                .limit(MAX_RESULTS)
                .map(t -> pathResult(t))
                .collect(Collectors.toList());
    }

    private PathResult pathResult(Tag t) {
        String path = getFullPath(t);
        return new PathResult(path, path);
    }

    public Tag resolveTagByPath(String dotPath) {
        if (dotPath == null || dotPath.isEmpty()) return null;
        String[] segments = dotPath.split("\\.");
        Long currentParentId = null;
        Tag current = null;

        for (String seg : segments) {
            if (seg.isEmpty()) return null;
            List<Tag> candidates = childrenByParentId.get(currentParentId);
            if (candidates == null) return null;
            current = candidates.stream()
                    .filter(t -> t.getNombre().equalsIgnoreCase(seg))
                    .findFirst().orElse(null);
            if (current == null) return null;
            currentParentId = current.getId();
        }
        return current;
    }

    public boolean pathExists(String dotPath) {
        return resolveTagByPath(dotPath) != null;
    }

    public boolean hasExactMatch(String name, Long parentId) {
        if (name == null || name.isBlank()) return false;
        List<Tag> siblings = childrenByParentId.get(parentId);
        if (siblings == null) return false;
        return siblings.stream().anyMatch(t -> t.getNombre().equalsIgnoreCase(name.trim()));
    }

    public String getFullPath(Tag t) {
        if (t == null) return "";
        String cached = fullPathCache.get(t.getId());
        if (cached != null) return cached;
        return buildPathFallback(t);
    }

    private String buildPathFallback(Tag t) {
        List<String> segments = new ArrayList<>();
        Tag current = t;
        while (current != null) {
            String cached = fullPathCache.get(current.getId());
            if (cached != null) {
                segments.add(0, cached);
                break;
            }
            segments.add(0, current.getNombre());
            current = tagById.get(current.getParentId());
        }
        String path = String.join(".", segments);
        fullPathCache.put(t.getId(), path);
        return path;
    }

    public Tag getParentTag(Tag t) {
        if (t == null || t.getParentId() == null) return null;
        return tagById.get(t.getParentId());
    }

    public List<Tag> getAllTags() {
        return allTags;
    }

    public List<Tag> getChildren(Long parentId) {
        return childrenByParentId.get(parentId);
    }
}
