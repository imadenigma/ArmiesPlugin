package me.imadenigma.armies

import me.imadenigma.armies.utils.colorize
import me.lucko.helper.Helper
import me.lucko.helper.Services
import me.lucko.helper.config.ConfigurationNode
import me.lucko.helper.config.yaml.YAMLConfigurationLoader
import me.lucko.helper.utils.Log
import kotlin.system.measureTimeMillis

class Configuration {
    var config: ConfigurationNode
        private set
    var language: ConfigurationNode
        private set
    var ranksConf: ConfigurationNode
        private set

    init {
        Log.info("&3Loading config files...".colorize())
        val ms = measureTimeMillis {
            val configFile = Helper.hostPlugin().getBundledFile("config.yml")
            val configLoader = YAMLConfigurationLoader.builder().setFile(configFile).build()

            val languageFile = Helper.hostPlugin().getBundledFile("language.yml")
            val languageLoader = YAMLConfigurationLoader.builder().setFile(languageFile).build()

            val ranksFile = Helper.hostPlugin().getBundledFile("ranksPermissions.yml")
            val ranksLoader = YAMLConfigurationLoader.builder().setFile(ranksFile).build()

            config = configLoader.load()
            language = languageLoader.load()
            ranksConf = ranksLoader.load()
        }
        Log.info("&3Loading took &4$ms &3ms".colorize())

        Services.provide(Configuration::class.java,this)
    }

    fun reload() {
        val configFile = Helper.hostPlugin().getBundledFile("config.yml")
        val configLoader = YAMLConfigurationLoader.builder().setFile(configFile).build()

        val languageFile = Helper.hostPlugin().getBundledFile("language.yml")
        val languageLoader = YAMLConfigurationLoader.builder().setFile(languageFile).build()

        val ranksFile = Helper.hostPlugin().getBundledFile("ranksPermissions.yml")
        val ranksLoader = YAMLConfigurationLoader.builder().setFile(ranksFile).build()

        config = configLoader.load()
        language = languageLoader.load()
        ranksConf = ranksLoader.load()
    }

}