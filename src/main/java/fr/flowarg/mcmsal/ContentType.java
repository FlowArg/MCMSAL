package fr.flowarg.mcmsal;

public enum ContentType
{
    APP_JSON("application/json"),
    APP_FORM_ENCODED("application/x-www-form-urlencoded");

    private final String contentType;

    ContentType(String contentType)
    {
        this.contentType = contentType;
    }

    public String getContentType()
    {
        return this.contentType;
    }
}
