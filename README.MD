[version]: https://img.shields.io/maven-central/v/fr.flowarg/mcmsal.svg?label=Download
[download]: https://search.maven.org/search?q=g:%22fr.flowarg%22%20AND%20a:%22mcmsal%22

[ ![version][] ][download]

# MCMSAL
MinecraftMicrosoft Authentication Library for Java 8+, adapted from MiniLauncher code (Java 11)

## Installation

### Gradle

In your block `repositories`, add this lines :
```groovy
repositories {
    mavenCentral()
}
```

and in your `dependencies` block :
```groovy
dependencies {
    implementation 'fr.flowarg:mcmsal:VERSION'
}
```

### Maven
```xml
<dependencies>
    <dependency>
      <groupId>fr.flowarg</groupId>
      <artifactId>mcmsal</artifactId>
      <version>VERSION</version>
    </dependency>
</dependencies>
```

## Usage

### Using JavaFX
There is a class named `JFXAuth` who can help you to use auth with Microsoft account (using a webview). There is an example :
```java
    // in a class
    private GridPane layout;


    // in a method
    JFXAuth.authenticateWithWebView(new JFXAuthCallback() {

        @Override
        public void beforeAuth(WebView webView)
        {
            MyClass.this.layout.getChildren().add(webView);
        }

        @Override
        public void webViewCanBeClosed(WebView webView)
        {
            MyClass.this.layout.getChildren().remove(webView);
        }

        @Override
        public Consumer<AuthInfo> onAuthFinished()
        {
            return (authInfo) -> {
                System.out.println(authInfo);
                // another actions
            };
        }

        @Override
        public void exceptionCaught(MCMSALException e)
        {
            e.printStackTrace();
        }

        @Override
        public double prefWidth()
        {
            return 405;
        }

        @Override
        public double prefHeight()
        {
            return 405;
        }
})
```

### Using Swing
There is no class who can help you to auth directly with swing. If you've found a solution, please make a pull request. If you have the `authCode`, you can use directly `MicrosoftAuthentication#authenticate(String, Consumer<AuthInfo>)` method. 
