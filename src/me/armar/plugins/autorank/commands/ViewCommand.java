package me.armar.plugins.autorank.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.armar.plugins.autorank.Autorank;
import me.armar.plugins.autorank.commands.manager.AutorankCommand;
import me.armar.plugins.autorank.language.Lang;
import me.armar.plugins.autorank.pathbuilder.Path;
import me.armar.plugins.autorank.pathbuilder.holders.RequirementsHolder;
import me.armar.plugins.autorank.pathbuilder.result.Result;
import me.armar.plugins.autorank.util.AutorankTools;

public class ViewCommand extends AutorankCommand {

	private final Autorank plugin;

	public ViewCommand(final Autorank instance) {
		this.setUsage("/ar view <path name>");
		this.setDesc("Gives a preview of a certain ranking path");
		this.setPermission("autorank.view");

		plugin = instance;
	}

	@Override
	public boolean onCommand(final CommandSender sender, final Command cmd, final String label, final String[] args) {

		// This command will give a preview of a certain path of ranking.
		if (!plugin.getCommandsManager().hasPermission("autorank.view", sender)) {
			return true;
		}

		if (args.length < 2) {
			sender.sendMessage(Lang.INVALID_FORMAT.getConfigValue("/ar view <path name> or /ar view list"));
			return true;
		}

		String pathName;

		boolean isPlayer = false;

		// Check if sender is player or console
		if (sender instanceof Player) {
			isPlayer = true;
		}

		// /ar view list (or a name of a path)
		if (args.length == 2) {

			pathName = AutorankTools.getStringFromArgs(args, 1);

			// Get a list of possible paths that a player can take?
			if (pathName.equals("list")) {

				final List<Path> paths = plugin.getPathManager().getPaths();

				if (paths.isEmpty()) {
					sender.sendMessage("There are no paths that you can choose.");
					return true;
				}

				sender.sendMessage(ChatColor.GREEN + "You can choose these paths: ");

				final String pathsString = AutorankTools.createStringFromList(paths);
				sender.sendMessage(ChatColor.WHITE + pathsString);
				return true;
			} else {
				// Third argument is probably a name of a path

				// Show details of path

				Path targetPath = plugin.getPathManager().matchPath(pathName, false);

				if (targetPath == null) {
					sender.sendMessage(Lang.NO_PATH_FOUND_WITH_THAT_NAME.getConfigValue());
					return true;
				}

				sender.sendMessage(ChatColor.GREEN
						+ "You can preview requirements (reqs), prerequisites (prereqs) or results (res) of this path.");
				sender.sendMessage(ChatColor.GOLD + "To view these, perform " + ChatColor.AQUA
						+ "/ar view reqs/prereqs/res " + targetPath.getDisplayName());

				return true;

			}

		} else if (args.length == 3) {
			// /ar view (req/prereq/result) (name of path)

			pathName = AutorankTools.getStringFromArgs(args, 2);
			String viewType = args[1];

			if (!viewType.contains("prereq") && !viewType.contains("req") && !viewType.contains("res")) {
				sender.sendMessage(Lang.INVALID_FORMAT.getConfigValue("/ar view reqs/prereqs/res <path name>"));
				return true;
			}

			Path targetPath = plugin.getPathManager().matchPath(pathName, false);

			if (targetPath == null) {
				sender.sendMessage(Lang.NO_PATH_FOUND_WITH_THAT_NAME.getConfigValue());
				return true;
			}

			if (viewType.contains("prereq")) {

				List<RequirementsHolder> holders = targetPath.getPrerequisites();

				sender.sendMessage("LENGTH: " + holders.size());
				
				// Set messages depending on console or player
				List<String> messages = (isPlayer
						? plugin.getPlayerChecker().formatRequirementsToList(holders,
								plugin.getPlayerChecker().getMetRequirementsHolders(holders, (Player) sender))
						: plugin.getPlayerChecker().formatRequirementsToList(holders, new ArrayList<Integer>()));

				sender.sendMessage(ChatColor.GREEN + "Prerequisites of path '" + ChatColor.GRAY
						+ targetPath.getDisplayName() + ChatColor.GREEN + "':");

				for (final String message : messages) {
					AutorankTools.sendColoredMessage(sender, message);
				}

				return true;

			} else if (viewType.contains("res")) {

				List<Result> results = targetPath.getResults();

				// Set messages depending on console or player
				List<String> messages = plugin.getPlayerChecker().formatResultsToList(results);

				sender.sendMessage(ChatColor.GREEN + "Results of path '" + ChatColor.GRAY + targetPath.getDisplayName()
						+ ChatColor.GREEN + "':");

				for (final String message : messages) {
					AutorankTools.sendColoredMessage(sender, message);
				}

				return true;
			} else {
				List<RequirementsHolder> holders = targetPath.getRequirements();

				// Set messages depending on console or player
				List<String> messages = (isPlayer
						? plugin.getPlayerChecker().formatRequirementsToList(holders,
								plugin.getPlayerChecker().getMetRequirementsHolders(holders, (Player) sender))
						: plugin.getPlayerChecker().formatRequirementsToList(holders, new ArrayList<Integer>()));

				sender.sendMessage(ChatColor.GREEN + "Requirements of path '" + ChatColor.GRAY
						+ targetPath.getDisplayName() + ChatColor.GREEN + "':");

				for (final String message : messages) {
					AutorankTools.sendColoredMessage(sender, message);
				}

				return true;
			}

		}
		return true;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * me.armar.plugins.autorank.commands.manager.AutorankCommand#onTabComplete(
	 * org.bukkit.command.CommandSender, org.bukkit.command.Command,
	 * java.lang.String, java.lang.String[])
	 */
	@Override
	public List<String> onTabComplete(final CommandSender sender, final Command cmd, final String commandLabel,
			final String[] args) {
		// TODO Auto-generated method stub
		final List<String> possibilities = new ArrayList<String>();

		// List shows a list of changegroups to view
		possibilities.add("list");

		for (final Path path : plugin.getPathManager().getPaths()) {
			possibilities.add(path.getDisplayName());
		}

		return possibilities;
	}

}
