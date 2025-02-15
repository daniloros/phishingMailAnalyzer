package model;

public class EmailFromBert {
    float[] embedding;
    int original_text_length;
    int num_tokens;

    public float[] getEmbedding() {
        return embedding;
    }

    public void setEmbedding(float[] embedding) {
        this.embedding = embedding;
    }

    public int getOriginal_text_length() {
        return original_text_length;
    }

    public void setOriginal_text_length(int original_text_length) {
        this.original_text_length = original_text_length;
    }

    public int getNum_tokens() {
        return num_tokens;
    }

    public void setNum_tokens(int num_tokens) {
        this.num_tokens = num_tokens;
    }
}
