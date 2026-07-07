package modelo.proyecto;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

public class CommentThread {

    private int estadoPr = 0;   // 0: sin msgs, 1: leido, 2: contestado, 3: nuevo
    private int estadoHtml = 0; // 0: sin msgs, 1: leido, 2: contestado, 3: nuevo
    private final List<Mensaje> mensajes = new ArrayList<>();

    public int getEstadoPr() {
        return estadoPr;
    }

    public void setEstadoPr(int estadoPr) {
        this.estadoPr = estadoPr;
    }

    public int getEstadoHtml() {
        return estadoHtml;
    }

    public void setEstadoHtml(int estadoHtml) {
        this.estadoHtml = estadoHtml;
    }

    public void add(String de, String texto, int iteracion) {
        mensajes.add(new Mensaje(de, texto, iteracion));
    }

    public boolean delete(int index, int currentIteration) {
        if (index < 0 || index >= mensajes.size()) return false;
        Mensaje msg = mensajes.get(index);
        if (!"nosotros".equals(msg.de()) || msg.isCompartido(currentIteration)) return false;
        mensajes.remove(index);
        return true;
    }

    public Mensaje get(int index) {
        return mensajes.get(index);
    }

    public Mensaje getLast() {
        return mensajes.isEmpty() ? null : mensajes.get(mensajes.size() - 1);
    }

    public int size() {
        return mensajes.size();
    }

    public boolean isEmpty() {
        return mensajes.isEmpty();
    }

    public List<Mensaje> getMessages() {
        return Collections.unmodifiableList(mensajes);
    }

    public String getLastText() {
        Mensaje last = getLast();
        return last != null ? last.texto() : "";
    }

    public void setMessages(List<Mensaje> nuevos) {
        mensajes.clear();
        if (nuevos != null) {
            mensajes.addAll(nuevos);
        }
    }

    public boolean hasMessages() {
        return !mensajes.isEmpty();
    }

    /**
     * Adaptador Gson que soporta tanto el formato antiguo (array plano de mensajes)
     * como el nuevo formato (objeto con estadoPr, estadoHtml y array de mensajes).
     */
    public static class GsonAdapter implements JsonSerializer<CommentThread>, JsonDeserializer<CommentThread> {

        private static final Type LIST_TYPE = new TypeToken<List<Mensaje>>() {}.getType();

        @Override
        public CommentThread deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            CommentThread ct = new CommentThread();
            
            if (json.isJsonArray()) {
                // Formato antiguo: Array plano. Calculamos estados por defecto para no romper nada
                List<Mensaje> mensajes = context.deserialize(json, LIST_TYPE);
                ct.setMessages(mensajes);
                if (mensajes.isEmpty()) {
                    ct.setEstadoPr(0);
                    ct.setEstadoHtml(0);
                } else {
                    Mensaje last = mensajes.get(mensajes.size() - 1);
                    if ("cliente".equalsIgnoreCase(last.de())) {
                        ct.setEstadoPr(3);   // Rojo (Nuevo para nosotros)
                        ct.setEstadoHtml(2); // Azul (Contestado por ellos)
                    } else {
                        ct.setEstadoPr(2);   // Azul (Contestado por nosotros)
                        ct.setEstadoHtml(3); // Rojo (Nuevo para ellos)
                    }
                }
            } else if (json.isJsonObject()) {
                // Formato nuevo: Objeto con estados explícitos
                JsonObject obj = json.getAsJsonObject();
                if (obj.has("estadoPr")) {
                    ct.setEstadoPr(obj.get("estadoPr").getAsInt());
                }
                if (obj.has("estadoHtml")) {
                    ct.setEstadoHtml(obj.get("estadoHtml").getAsInt());
                }
                if (obj.has("mensajes")) {
                    List<Mensaje> mensajes = context.deserialize(obj.get("mensajes"), LIST_TYPE);
                    ct.setMessages(mensajes);
                }
            }
            return ct;
        }

        @Override
        public JsonElement serialize(CommentThread src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject obj = new JsonObject();
            obj.addProperty("estadoPr", src.getEstadoPr());
            obj.addProperty("estadoHtml", src.getEstadoHtml());
            obj.add("mensajes", context.serialize(src.getMessages()));
            return obj;
        }

    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommentThread that = (CommentThread) o;
        return estadoPr == that.estadoPr && estadoHtml == that.estadoHtml
                && Objects.equals(mensajes, that.mensajes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(estadoPr, estadoHtml, mensajes);
    }

} // --- Fin de clase CommentThread ---

