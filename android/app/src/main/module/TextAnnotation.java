public class TextAnnotation {
    // public final String locale;
    public final String description;
    public final int[][] vertices;

    public TextAnnotation(String description, int[][] vertices) {
        // this.locale = locale;
        this.description = description;
        this.vertices = vertices;
    }
}