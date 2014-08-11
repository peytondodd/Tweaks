package com.github.intangir.Tweaks;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class Chat extends Tweak
{
	Chat(Tweaks plugin) {
		super(plugin);
		CONFIG_FILE = new File(plugin.getDataFolder(), "chat.yml");
		TWEAK_NAME = "Tweak_Chat";
		TWEAK_VERSION = "1.0";
		
		censors = new HashMap<String, String>();
		censors.put("fuck", "frak");
		censors.put("shit", "poop");
		censors.put("dick", "dork");
		censors.put("cock", "cork");
		censors.put("cunt", "creep");
		censors.put("nigg", "nagg");
		
		globalPermission = "tweak.chat.globalchat";
		localDistance = 96; // 8 chunks
		
		retells = new HashMap<String, String>();
		replies = new HashMap<String, String>();
		ignores = new HashMap<String, Set<String>>();
		
		localMode = new HashSet<String>();
		
	}
	
	public void delayedEnable()
	{
		String pattern = join(new ArrayList<String>(censors.keySet()), "|");
		censorsPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);

		// ensure these aren't hidden even if mod is hidden in help.yml
		Commands.unhideCommands(Arrays.asList("l", "g", "tell", "r", "rt", "ignore", "unignore"));
		Commands.addAlias("/local", "/l");
		Commands.addAlias("/global", "/g");
		Commands.addAlias("/retell", "/rt");
		Commands.addAlias("/reply", "/r");
		Commands.addAlias("/msg", "/tell");
		Commands.addAlias("/w ", "/tell ");
	}
	
	public String censor(String message) {
		
		Matcher m = censorsPattern.matcher(message);
	
		while(m.find()) {
			message = message.replaceAll(m.group(), censors.get(m.group().toLowerCase()));
		}
		return message;
	}
	
	private transient Map<String, String> retells;
	private transient Map<String, String> replies;
	private transient Map<String, Set<String>> ignores;
	private Map<String, String> censors;
	private transient Set<String> localMode;
	private transient Pattern censorsPattern;
	private String globalPermission;
	private int localDistance;
	

	@EventHandler(ignoreCancelled = true)
	public void onPlayerChat(AsyncPlayerChatEvent e) {
		e.setMessage(censor(e.getMessage()));
		
		Player sender = e.getPlayer();
		if(sender == null)
			return;
		
		Set<Player> recipients = e.getRecipients();
		
		// range limiting
		if(!sender.hasPermission(globalPermission) || localMode.contains(sender.getName())) {
			e.setMessage(ChatColor.GRAY + e.getMessage());
			
			// get nearby players
			Set<Player> nearby = new HashSet<Player>();
			for(Player p : sender.getWorld().getPlayers()) {
				if(p.getLocation().distanceSquared(sender.getLocation()) < localDistance * localDistance) {
					nearby.add(p);
				}
			}
			recipients.retainAll(nearby);
		}
		
		// remove ignored
		if(ignores.containsKey(sender.getName())) {
			for(String ignorer : ignores.get(sender.getName())) {
				Player p = server.getPlayer(ignorer);
				if(p != null){
					recipients.remove(p);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerDeath(PlayerDeathEvent e) {
		// censor incase someone has a creatively named weapon
		String msg = censor(e.getDeathMessage());
		
		// color the text in red
		e.setDeathMessage(ChatColor.RED + msg);
		
		// append location info for logging and private message
		Location loc = e.getEntity().getLocation();
		msg = msg + "([" + loc.getWorld().getName() + "] " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() + ")";
		log.info(msg);
		e.getEntity().sendMessage(ChatColor.YELLOW + msg);
	}
	
	void MessagePlayer(CommandSender sender, CommandSender receiver, String message) {
		if(ignores.containsKey(sender.getName()) && ignores.get(sender.getName()).contains(receiver.getName())) {
			sender.sendMessage(ChatColor.RED + "You are being ignored.");
			return;
		}
		
		receiver.sendMessage(ChatColor.GREEN + "<from " + sender.getName() + "> " + message);
		sender.sendMessage(ChatColor.DARK_GREEN + "<to " + receiver.getName() + "> " + message);
		log.info("<" + sender.getName() + " to " + receiver.getName() + "> " + message);
		
		retells.put(sender.getName(), receiver.getName());
		replies.put(receiver.getName(), sender.getName());
	}

	@CommandHandler("l")
	public void onLocal(CommandSender sender, String[] args) {
		if(sender instanceof Player) {
			Player p = (Player) sender;

			localMode.add(p.getName());

			if(args.length > 0) {
				p.chat(join(Arrays.asList(args), " "));
			} else {
				sender.sendMessage(ChatColor.YELLOW + "Focused Channel [l. Local]");
			}
		}
	}

	@CommandHandler("g")
	public void onGlobal(CommandSender sender, String[] args) {
		if(sender instanceof Player) {
			Player p = (Player) sender;

			localMode.remove(p.getName());

			if(args.length > 0) {
				p.chat(join(Arrays.asList(args), " "));
			} else {
				if(!sender.hasPermission(globalPermission)) {
					sender.sendMessage(ChatColor.RED + "You don't have permission to use global chat");
				} else {
					sender.sendMessage(ChatColor.YELLOW + "Focused Channel [g. Global]");
				}
			}
		}
	}

	@CommandHandler("tell")
	public void onTell(CommandSender sender, String[] args) {
		if(args.length < 2) {
			sender.sendMessage(ChatColor.RED + "Usage: /tell <player> <message>");
			return;
		}
		
		Player player = server.getPlayer(args[0]);
		
		if(player != null) {
			List<String> list = Arrays.asList(args);
			MessagePlayer(sender, player, join(list.subList(1, list.size()), " "));
		} else {
			sender.sendMessage(ChatColor.RED + "There is no player by that name online.");
		}
	}
	
	@CommandHandler("r")
	public void onReply(CommandSender sender, String[] args) {
		if(args.length < 1) {
			sender.sendMessage(ChatColor.RED + "Usage: /reply <message>");
			return;
		}
		
		if(replies.containsKey(sender.getName())) {
			Player player = server.getPlayer(replies.get(sender.getName()));
			if(player != null) {
				MessagePlayer(sender, player, join(Arrays.asList(args), " "));
				return;
			}
		}
		
		sender.sendMessage(ChatColor.RED + "No one to reply to.");
	}
	
	@CommandHandler("rt")
	public void onRetell(CommandSender sender, String[] args) {
		if(args.length < 1) {
			sender.sendMessage(ChatColor.RED + "Usage: /retell <message>");
			return;
		}
		
		if(retells.containsKey(sender.getName())) {
			Player player = server.getPlayer(retells.get(sender.getName()));
			if(player != null) {
				MessagePlayer(sender, player, join(Arrays.asList(args), " "));
				return;
			}
		}
		
		sender.sendMessage(ChatColor.RED + "No one to retell to.");
	}

	@CommandHandler("ignore")
	public void onIgnore(CommandSender sender, String[] args) {
		if(args.length != 1) {
			sender.sendMessage(ChatColor.RED + "Usage: /ignore <player>");
			return;
		}
		
		if(!ignores.containsKey(args[0])) {
			ignores.put(args[0], new HashSet<String>());
		}
		
		if(ignores.get(args[0]).contains(sender.getName())) {
			// already ignored? toggle to unignore
			onUnignore(sender, args);
		} else {
			ignores.get(args[0]).add(sender.getName());
			sender.sendMessage(ChatColor.YELLOW + args[0] + " is being ignored.");
		}
	}
	
	@CommandHandler("unignore")
	public void onUnignore(CommandSender sender, String[] args) {
		if(args.length != 1) {
			sender.sendMessage(ChatColor.RED + "Usage: /unignore <player>");
			return;
		}
		
		try {
			ignores.get(args[0]).remove(sender.getName());
		} catch(Exception e) {}
		
		sender.sendMessage(ChatColor.YELLOW + args[0] + " is not being ignored.");
	}
}