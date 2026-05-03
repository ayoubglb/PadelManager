package be.ephec.padelmanager.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

//  Active les jobs planifiés
//  Le bean Clock est défini dans ClockConfig
@Configuration
@EnableScheduling
public class SchedulingConfig {
}