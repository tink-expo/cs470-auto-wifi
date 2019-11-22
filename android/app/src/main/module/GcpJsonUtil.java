import com.google.gson.*;

public class GcpJsonUtil {
    static final JsonParser parser = new JsonParser();

    public static IdPwPicker GetIdPwPickerFromGcpJsonResponse(String gcpJsonResponse) {
        JsonElement element = parser.parse(gcpJsonResponse);

        try {

            JsonObject response = element.getAsJsonObject().get("responses").getAsJsonArray().get(0).getAsJsonObject();

            JsonObject pageObject =
                    response.get("fullTextAnnotation").getAsJsonObject()
                    .get("pages").getAsJsonArray().get(0).getAsJsonObject();
            int imageWidth = pageObject.get("width").getAsInt();
            int imageHeight = pageObject.get("height").getAsInt();

            JsonArray annotationJsonArray = response.get("textAnnotations").getAsJsonArray();
            if (annotationJsonArray.size() <= 1) {
                return null;
            }
            IdPwPicker.TextAnnotation[] annotations = new IdPwPicker.TextAnnotation[annotationJsonArray.size() - 1];
            for (int i = 1; i < annotationJsonArray.size(); ++i) {
                 JsonObject annotationJsonObject = annotationJsonArray.get(i).getAsJsonObject();
                 String description = annotationJsonObject.get("description").getAsString();
                 int[][] vertices = new int[4][2];
                 JsonArray verticesJsonArray =
                         annotationJsonObject.get("boundingPoly").getAsJsonObject()
                         .get("vertices").getAsJsonArray();
                 for (int v = 0; v < 4; ++v) {
                     JsonObject vertexJsonObject = verticesJsonArray.get(v).getAsJsonObject();
                     vertices[v][0] = vertexJsonObject.get("x").getAsInt();
                     vertices[v][1] = vertexJsonObject.get("y").getAsInt();
                 }
                 annotations[i - 1] = new IdPwPicker.TextAnnotation(description, vertices);
            }

            return new IdPwPicker(annotations, imageWidth, imageHeight);

        } catch (NullPointerException e) {

            return null;

        } catch (ArrayIndexOutOfBoundsException e) {

            return null;

        }
    }
}
