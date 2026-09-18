package org.mtr.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mtr.mod.generated.WebserverResources;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class WebserverResourcesTest {

	@Test
	public void includesCreatorPageAndBundles() {
		final String index = WebserverResources.get("index.html");
		Assertions.assertNotNull(index, "Build and embed the creator website before running tests");
		Assertions.assertTrue(index.contains("<base href=\"./\">"));
		Assertions.assertEquals(index, WebserverResources.get("/index.html"));
		final Matcher bundles = Pattern.compile("(?:src|href)=\"([^\"]+\\.(?:js|css))\"").matcher(index);
		int bundleCount = 0;
		while (bundles.find()) {
			final String resource = bundles.group(1);
			Assertions.assertNotNull(WebserverResources.get(resource), "Missing embedded bundle: " + resource);
			bundleCount++;
		}
		Assertions.assertTrue(bundleCount > 0);
		Assertions.assertNull(WebserverResources.get("not-a-resource.js"));
	}

	@Test
	public void preservesBuiltWebsiteContent() throws IOException {
		final Path website = Path.of("../website/dist/website/browser");
		Assertions.assertTrue(Files.isDirectory(website), "Build the website before running tests");
		try (final Stream<Path> files = Files.walk(website)) {
			for (final Path file : files.filter(Files::isRegularFile).toList()) {
				final String resource = website.relativize(file).toString().replace('\\', '/');
				Assertions.assertEquals(Files.readString(file), WebserverResources.get(resource), resource);
			}
		}
	}
}
