package fr.flowarg.mcmsal;

import org.json.simple.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Function;

public enum DataType
{
    FORM_DATA((data) -> {
        final StringBuilder builder = new StringBuilder();
        try
        {
            for (Map.Entry<Object, Object> entry : data.entrySet())
            {
                if (builder.length() > 0) builder.append("&");
                builder.append(URLEncoder.encode(entry.getKey().toString(), StandardCharsets.UTF_8.name()));
                builder.append("=");
                builder.append(URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8.name()));
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return builder.toString();
    }),
    JSON_DATA((data) -> new JSONObject(data).toJSONString());

    private final Function<Map<Object, Object>, String> function;

    DataType(Function<Map<Object, Object>, String> function)
    {
        this.function = function;
    }

    public Function<Map<Object, Object>, String> getFunction()
    {
        return this.function;
    }
}
