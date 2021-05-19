package me.imadenigma.armies

import me.lucko.helper.Helper
import me.lucko.helper.Services
import me.lucko.helper.config.ConfigurationNode
import me.lucko.helper.config.ConfigurationOptions
import me.lucko.helper.config.loader.ConfigurationLoader
import me.lucko.helper.config.yaml.YAMLConfigurationLoader
import me.lucko.helper.utils.Log
import java.io.File
import kotlin.system.measureTimeMillis

class Configuration {
    var config: ConfigurationNode
        private set
    var language: ConfigurationNode
        private set

    init {
        Log.info("&3Loading config files...".colorize())
        val ms = measureTimeMillis {
            val configFile = Helper.hostPlugin().getBundledFile("config.yml")
            val languageFile = Helper.hostPlugin().getBundledFile("language.yml")

            val configLoader = YAMLConfigurationLoader.builder().setFile(configFile).build()
            val languageLoader = YAMLConfigurationLoader.builder().setFile(languageFile).build()

            config = configLoader.load()
            language = languageLoader.load()
        }
        Log.info("&3Loading took &4$ms &3ms".colorize())

        Services.provide(Configuration::class.java,this)
    }

    fun reload() {
        val configFile = Helper.hostPlugin().getBundledFile("config.yml")
        val languageFile = Helper.hostPlugin().getBundledFile("language.yml")

        val configLoader = YAMLConfigurationLoader.builder().setFile(configFile).build()
        val languageLoader = YAMLConfigurationLoader.builder().setFile(languageFile).build()

        config = configLoader.load()
        language = languageLoader.load()
    }

}