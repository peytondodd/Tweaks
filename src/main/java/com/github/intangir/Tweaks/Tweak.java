package com.github.intangir.Tweaks;

import java.util.logging.Logger;

import net.cubespace.Yamler.Config.Config;
import net.cubespace.Yamler.Config.InvalidConfigurationException;

import org.bukkit.event.Listener;

public class Tweak extends Config implements Listener 
{
	transient Tweaks plugin;
	transient Logger log;
	transient String TWEAK_NAME;
	transient String TWEAK_VERSION;

	Tweak(Tweaks plugin) {
		this.plugin = plugin;
		this.log = plugin.getLog();
	}
	
	public void enable() {
		log.info("Enabling " + TWEAK_NAME + " v" + TWEAK_VERSION);
		init();
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	public void disable() {
		log.info("Disabling " + TWEAK_NAME + " v" + TWEAK_VERSION);
	}
	
	@Override
	public void init() {
		try {
			super.init();
		} catch (InvalidConfigurationException e) {
			log.info("Couldn't Load " + CONFIG_FILE);
		}
	}

	@Override
	public void save() {
		try {
			super.save();
		} catch (InvalidConfigurationException e) {
			log.info("Couldn't Save " + CONFIG_FILE);
		}
	}
}
