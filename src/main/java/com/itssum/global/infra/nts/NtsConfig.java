package com.itssum.global.infra.nts;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(NtsProperties.class)
public class NtsConfig {
}
