package com.academic.integrity.review.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

	private Path uploadsDir = Paths.get("uploads");
}