package fr.flowarg.mcmsal;

import javafx.collections.ListChangeListener;
import javafx.scene.layout.GridPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;

import java.util.function.Consumer;

/**
 * Class to use if you want to authenticate the user with a JavaFX interface.<br>
 * Example code:
 * <pre><code>
    // in a class
    private {@link GridPane} layout;


    // in a method
    JFXAuth.authenticateWithWebView(new JFXAuthCallback() {
        public void beforeAuth({@link WebView} webView)
        {
            MyClass.this.layout.getChildren().add(webView);
        }

        public void webViewCanBeClosed({@link WebView} webView)
        {
            MyClass.this.layout.getChildren().remove(webView);
        }

        public Consumer&lt;{@link AuthInfo}&gt; onAuthFinished()
        {
            return (authInfo) -&gt; {
                System.out.println(authInfo);
                // another actions
            };
        }

        public void exceptionCaught({@link MCMSALException} e)
        {
            e.printStackTrace();
        }

        public double prefWidth()
        {
            return 405;
        }

        public double prefHeight()
        {
            return 405;
        }
})
 * </code></pre>
 * @see #authenticateWithWebView(JFXAuthCallback) 
 */
public class JFXAuth
{
    public static void authenticateWithWebView(JFXAuthCallback callback)
    {
        final WebView webView = new WebView();
        final WebEngine webEngine = webView.getEngine();

        webEngine.load("https://login.live.com/oauth20_authorize.srf" +
                               "?client_id=00000000402b5328" +
                               "&response_type=code" +
                               "&scope=service%3A%3Auser.auth.xboxlive.com%3A%3AMBI_SSL" +
                               "&redirect_uri=https%3A%2F%2Flogin.live.com%2Foauth20_desktop.srf");
        webEngine.setJavaScriptEnabled(true);
        webView.setPrefWidth(callback.prefWidth());
        webView.setPrefHeight(callback.prefHeight());
        webEngine.getHistory().getEntries().addListener((ListChangeListener<WebHistory.Entry>) c -> {
            if (c.next() && c.wasAdded())
            {
                c.getAddedSubList().forEach(entry -> {
                    try
                    {
                        if (entry.getUrl().startsWith("https://login.live.com/oauth20_desktop.srf?code="))
                        {
                            final String authCode = entry.getUrl().substring(entry.getUrl().indexOf("=") + 1, entry.getUrl().indexOf("&"));
                            callback.webViewCanBeClosed(webView);
                            new MicrosoftAuthentication().authenticate(authCode, callback.onAuthFinished());
                        }
                    } catch (MCMSALException e)
                    {
                        callback.exceptionCaught(e);
                    }
                });
            }
        });
        callback.beforeAuth(webView);
    }

    public interface JFXAuthCallback
    {
        void beforeAuth(WebView webView);
        void webViewCanBeClosed(WebView webView);
        Consumer<AuthInfo> onAuthFinished();
        void exceptionCaught(MCMSALException e);
        double prefWidth();
        double prefHeight();
    }
}
