package no.detolly.TestPlugin;

import java.util.HashMap;
import java.util.Random;
import java.util.UUID;
import java.sql.*;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public class TestPlugin extends JavaPlugin implements Listener {

	private Logger logger;
	private PluginManager manager;
	
	private HashMap<Player, Boolean> fireEnabled;
	private HashMap<Player, Boolean> godEnabled;
	
	private Random rand = new Random();
	
	public static Connection c = null;
    Statement currentSqlCommand = null;
	
	@Override
    public void onEnable() {
		logger = getLogger();
		fireEnabled = new HashMap<Player, Boolean>();
		godEnabled = new HashMap<Player, Boolean>();
		logger.info("Enabling TestPlugin");
		manager = getServer().getPluginManager();
	    manager.registerEvents(this, this);
	    
	    try {
	    	Class.forName("org.sqlite.JDBC");
		    c = DriverManager.getConnection("jdbc:sqlite:TestPlugin.db");
		    logger.info(c == null ? "Database connection not created." : "Database connection successfully created.");
	    } catch(Exception e) {
	    	logger.info("Failed to connect to the database.");
	    }
    	String sql = "CREATE TABLE IF NOT EXISTS `players` (" + 
    			"  `uuid` varchar(30) NOT NULL," + 
    			"  `homex` int(4) DEFAULT null," + 
    			"  `homey` int(4) DEFAULT null," + 
    			"  `homez` int(4) DEFAULT null," + 
    			"  `homeworld` varchar(60) DEFAULT null," + 
    			"  `lastx` int(4) DEFAULT null," + 
    			"  `lasty` int(4) DEFAULT null," + 
    			"  `lastz` int(4) DEFAULT null," + 
    			"  `lastworld` varchar(60) DEFAULT null," + 
    			"  `timber` TINYINT(1) DEFAULT 1," + 
    			"  `ores` TINYINT(1) DEFAULT 1," + 
    			"  PRIMARY KEY (`uuid`)" + 
    			");";
    	doSQLUpdate(sql);
    	String sql2 = "CREATE TABLE IF NOT EXISTS `warps` ("
    			+ "`creatorUUID` varchar(30) NOT NULL,"
    			+ "`name` varchar(30) NOT NULL UNIQUE,"
    			+ "`description` MEDIUMTEXT NOT NULL,"
    			+ "`x` INT(5) NOT NULL,"
    			+ "`y` INT(5) NOT NULL,"
    			+ "`z` INT(5) NOT NULL,"
    			+ "`world` varchar(30) NOT NULL,"
    			+ "PRIMARY KEY (`name`)"
    			+ ");";
    	doSQLUpdate(sql2);
    }
    
    @Override
    public void onDisable() {
		logger.info("Disabling TestPlugin");
		try {
			c.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (sender instanceof Player) {
			//player commands
			Player p  = (Player)sender;
			if (cmd.getName().equalsIgnoreCase("fire")) {
				if (fireEnabled.get(p) == null) {
					fireEnabled.put(p, true);
				} else {
					fireEnabled.replace(p, fireEnabled.get(p), !fireEnabled.get(p));
				}
				return true;
			} else if (cmd.getName().equalsIgnoreCase("god")) {
				if (godEnabled.get(p) == null) {
					godEnabled.put(p, true);
				} else {
					godEnabled.replace(p, godEnabled.get(p), !godEnabled.get(p));
				}
				return true;
			} else if (cmd.getName().equalsIgnoreCase("info")) {
				try {
					currentSqlCommand = c.createStatement();
					ResultSet rs = currentSqlCommand.executeQuery("SELECT * FROM players WHERE `uuid`== \"" + p.getUniqueId() + "\"");
					rs.next();
					p.sendMessage("UUID: " + rs.getString("uuid"));
					Vector v = new Vector(rs.getInt("homex"), rs.getInt("homey"), rs.getInt("homez"));
					Vector u = new Vector(rs.getInt("lastx"), rs.getInt("lasty"), rs.getInt("lastz"));
					p.sendMessage("Home Coords: " + v.toString());
					p.sendMessage("Back Coords: " + u.toString());
					currentSqlCommand.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return true;
			} else if (cmd.getName().equalsIgnoreCase("sethome")) {
				String sss = "UPDATE players SET `homex`="+p.getLocation().getBlockX()+", `homey`="+p.getLocation().getBlockY()+", `homez`="+p.getLocation().getBlockZ()+ ", `homeworld`=\""+p.getLocation().getWorld().getUID().toString()+"\"" + " WHERE `uuid`=\""+p.getUniqueId()+"\"";
				doSQLUpdate(sss);
				p.sendMessage("Set Home to " + p.getLocation().getBlockX() + " " + p.getLocation().getBlockY() + " " + p.getLocation().getBlockZ());
				return true;
			} else if (cmd.getName().equalsIgnoreCase("home")) {
				try {
					currentSqlCommand = c.createStatement();
					ResultSet rs = currentSqlCommand.executeQuery("SELECT * FROM players WHERE `uuid`== \"" + p.getUniqueId() + "\"");
					Vector v = null;
					World world = null;
					try {
						int x  = rs.getInt("homex");
						int y = rs.getInt("homey");
						int z = rs.getInt("homez");
						String w = rs.getString("homeworld");
						world = getServer().getWorld(UUID.fromString(w));
						v = new Vector(x, y, z);
					} catch (Exception e) {
						e.printStackTrace();
						p.sendMessage("You must set a home first!");
						return true;
					}
					v.add(new Vector(0.5f,0.5f,0.5f));
					p.teleport(v.toLocation(world, p.getLocation().getYaw(), p.getLocation().getPitch()));
					p.sendMessage("Teleported to home location.");
					currentSqlCommand.close();
				} catch(Exception e) {
					
				}
				return true;
			} else if (cmd.getName().equalsIgnoreCase("timber")) {
				if (args.length == 0) {
					try {
					currentSqlCommand = c.createStatement();
					ResultSet rs = currentSqlCommand.executeQuery("SELECT * FROM players WHERE `uuid`=\"" + p.getUniqueId() + "\"");
					rs.next();
					p.sendMessage("Your current timbper status is: " + rs.getBoolean("timber"));
					currentSqlCommand.close();
					} catch(Exception e) {
						e.printStackTrace();
						//unhandled
					}
				} else if (args.length == 1) {
					if (args[0].equals("true") || args[0].equals("false")) {
						int bool = (args[0].equals("true") ? 1 : 0);
						String sql = "UPDATE players SET timber = " + bool  + " WHERE uuid=\"" + p.getUniqueId() + "\"";
						doSQLUpdate(sql);
						p.sendMessage("Successfully upated timber.");
					} else {
						try {
							@SuppressWarnings("deprecation")
							Player targetPlayer = getServer().getPlayer(args[0]);
							currentSqlCommand = c.createStatement();
							ResultSet rs = currentSqlCommand.executeQuery("SELECT * FROM players WHERE `uuid`=\"" + targetPlayer.getUniqueId() + "\"");
							rs.next();
							p.sendMessage(targetPlayer.getDisplayName() + "'s current timber status is: " + rs.getBoolean("timber"));
							currentSqlCommand.close();
							p.sendMessage("Usage: " + cmd.getUsage());
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
				return true;
			} else if (cmd.getName().equalsIgnoreCase("back")) {
				try {
					currentSqlCommand = c.createStatement();
					ResultSet rs = currentSqlCommand.executeQuery("SELECT * FROM players WHERE `uuid`== \"" + p.getUniqueId() + "\"");
					Vector v = null;
					World world = null;
					try {
						int x  = rs.getInt("lastx");
						int y = rs.getInt("lasty");
						int z = rs.getInt("lastz");
						world = getServer().getWorld(UUID.fromString(rs.getString("lastworld")));
						v = new Vector(x, y, z);
					} catch (Exception e) {
						p.sendMessage("Last location not defined.");
						return true;
					}
					v.add(new Vector(0.5f,0.5f,0.5f));
					//p.teleport(new Location(world, v.getX(), v.getY(), v.getZ()));
					p.sendMessage(world.getName() + ": " + v.toString());
					p.teleport(v.toLocation(world, p.getLocation().getYaw(), p.getLocation().getPitch()));
					p.sendMessage("Teleported to last location.");
					currentSqlCommand.close();
				} catch(Exception e) {
					
				}
				return true;
			} else if (cmd.getName().equalsIgnoreCase("ores")) {
				if (args.length == 0) {
					try {
					currentSqlCommand = c.createStatement();
					ResultSet rs = currentSqlCommand.executeQuery("SELECT * FROM players WHERE `uuid`=\"" + p.getUniqueId() + "\"");
					rs.next();
					p.sendMessage("Your current ore status is: " + rs.getBoolean("ores"));
					currentSqlCommand.close();
					} catch(Exception e) {
						e.printStackTrace();
						//unhandled
					}
				} else if (args.length == 1) {
					if (args[0].equals("true") || args[0].equals("false")) {
						int bool = (args[0].equalsIgnoreCase("true") ? 1 : 0);
						String sql = "UPDATE players SET ores = " + bool  + " WHERE uuid=\"" + p.getUniqueId() + "\"";
						doSQLUpdate(sql);
						p.sendMessage("Successfully upated ores.");
					} else {
						try {
							@SuppressWarnings("deprecation")
							Player targetPlayer = getServer().getPlayer(args[0]);
							currentSqlCommand = c.createStatement();
							ResultSet rs = currentSqlCommand.executeQuery("SELECT * FROM players WHERE `uuid`=\"" + targetPlayer.getUniqueId() + "\"");
							rs.next();
							p.sendMessage(targetPlayer.getDisplayName() + "'s current ore status is: " + rs.getBoolean("ores"));
							currentSqlCommand.close();
							p.sendMessage("Usage: " + cmd.getUsage());
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
				return true;
			} else if (cmd.getName().equalsIgnoreCase("warp")) {
				// /<warp> <create/remove> <name> [description] 
				// INSERT (creatorUUID, name, description, x, y, z, world)
				if (args.length == 0) {
					p.sendMessage("Syntax error, usage: " + cmd.getUsage());
					return true;
				} else if (args.length == 1) {
					p.sendMessage("Syntax error, usage: " + cmd.getUsage());
					return true;
				} else if (args.length > 1) {
					if (args[0].equalsIgnoreCase("create"))
						try {
							String creatorUUID = p.getUniqueId().toString();
							String name = args[1];
							String description = concat(args, " ", 2);
							int x = p.getLocation().getBlockX();
							int y = p.getLocation().getBlockY();
							int z = p.getLocation().getBlockZ();
							String worldUUID = p.getWorld().getUID().toString();
							currentSqlCommand = c.createStatement();
							ResultSet rs = currentSqlCommand.executeQuery("SELECT * FROM `warps` WHERE name=\"" + name + "\"");
							if (rs.next()) {
								p.sendMessage("A warp with that name already exists.");
								return true;
							}
							currentSqlCommand.close();
							currentSqlCommand = c.createStatement();
							String sql = "INSERT INTO warps (creatorUUID, name, description, x, y, z, world) VALUES (\""+creatorUUID+"\", \""+name+"\", \""+description+"\", "+x+", "+y+", "+z+", \""+worldUUID+"\")";
							currentSqlCommand.executeUpdate(sql);
							currentSqlCommand.close();
							p.sendMessage("Successfully created warp.");
							return true;
						} catch (Exception e) {
							e.printStackTrace();
							p.sendMessage("Something went horribly wrong.");
							return true;
						}
					if (args[0].equalsIgnoreCase("remove")) {
						try {
							ResultSet rs = currentSqlCommand.executeQuery("SELECT * FROM `warps` WHERE name=\"" + args[1] + "\"");
							if (rs.next()) {
								currentSqlCommand = c.createStatement();
								currentSqlCommand.executeUpdate("DELETE FROM `warps` WHERE name=\""+args[1]+"\"");
								currentSqlCommand.close();
								p.sendMessage("Successfully removed warp.");
								return true;
							} else {
								p.sendMessage("A warp with that name does not exist.");
								return true;
							}
						} catch(Exception e) {
							e.printStackTrace();
						}
					}
					p.sendMessage("gree");
					return true;
				}
			} else if (cmd.getName().equalsIgnoreCase("warps")) {
				/*
				 * Current Warps:
				 *  -name: description
				 *  -name2: description2
				 *  -name3: description3
				 * Showing page x of y 
				 */
				int page = 0;
				if (args.length == 0)
					page = 1;
				else if (args.length > 0)
					try {
					page = Integer.parseInt(args[0]);
					} catch(NumberFormatException e) {
						p.sendMessage("Page must be a number.");
						return true;
					}
				try {
					currentSqlCommand = c.createStatement();
					String message = "§f-----------------------§l§6§nCurrent Warps:§f-----------------\n";
						//message+="§7-----------------------------------------------------§f";
					ResultSet rs = currentSqlCommand.executeQuery("SELECT * FROM `warps` WHERE ROWID > " + (page*5-5) + " LIMIT 8");
					while(rs.next()) {
						message+=" §a- ";
						message+=rs.getString("name") + ": §f" + rs.getString("description");
						message+="\n";
					}
					message+="§7Showing page " + page + ".";
					p.sendMessage(message);
					return true;
				} catch(Exception e) {
					e.printStackTrace();
					p.sendMessage("Error.");
					return true;
				}
				
			} else if (cmd.getName().equalsIgnoreCase("to")) {
				if (args.length == 0) { 
					p.sendMessage("Incorrect Syntax: " + cmd.getUsage());
				} else {
					try {
					String name = args[0];
					currentSqlCommand = c.createStatement();
					ResultSet rs = currentSqlCommand.executeQuery("SELECT * FROM warps WHERE name=\"" + name + "\"");
					Vector v = null;
					World w = null;
					if (rs.next()) {
						int x = rs.getInt("x");
						int y = rs.getInt("y");
						int z = rs.getInt("z");
						v = new Vector(x,y,z);
						w = getServer().getWorld(UUID.fromString(rs.getString("world")));
					} else {
						p.sendMessage("Warp not found.");
						return true;
					}
					p.teleport(v.toLocation(w, p.getLocation().getYaw(), p.getLocation().getPitch()));
					p.sendMessage("Zoop!");
					currentSqlCommand.close();
					return true;
					} catch (Exception e) {
						e.printStackTrace();
						return false;
					}
				}
			}
		}
		else {
			//console commands
		}
		//both commands
		return false;
	}
	
	private String concat(String[] arr, String key, int startvalue) {
		String returnthis = "";
		for(int i = startvalue; i < arr.length-1; i++) {
			returnthis+=arr[i];
			if (i != arr.length-1)
				returnthis+=key;
		}
		return returnthis;
	}
	
	private int doSQLUpdate(String sql) {
		try {
			currentSqlCommand = c.createStatement();
			int a = currentSqlCommand.executeUpdate(sql);
			currentSqlCommand.close();
			return a;
		} catch(Exception e) {
			System.out.println(e.getMessage());
			return 0;
		}
	}
	
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		Player p = event.getPlayer();
		if (fireEnabled.get(p) != null && fireEnabled.get(p)) {
			Vector v = new Vector(0,1,0);
			p.getLocation().subtract(v).getBlock().setType(Material.NETHERRACK);
			p.getLocation().getBlock().setType(Material.FIRE);
		}
		if (godEnabled.get(p) != null && godEnabled.get(p)) {
			Vector blockLocation = p.getLocation().toVector();
			Vector v = new Vector(0,(p.isSneaking()) ? 2 : 1,0);
			Location l = (blockLocation.subtract(v)).toLocation(p.getWorld());
			if (p.getWorld().getBlockAt(l).getType() == Material.AIR || p.getWorld().getBlockAt(l).isLiquid())
				p.getWorld().getBlockAt(l).setType(Material.STONE);
		}
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent evt) {
		Player p = evt.getPlayer();
	    try {
			currentSqlCommand = c.createStatement();
			ResultSet rs = currentSqlCommand.executeQuery("SELECT * FROM players WHERE `uuid`== \"" + p.getUniqueId() + "\"");
			rs.next();
			if (!rs.next()) {
				currentSqlCommand = c.createStatement();
				currentSqlCommand.executeUpdate("INSERT INTO players (`uuid`)" + " VALUES (\""+p.getUniqueId()+"\")");
				currentSqlCommand.close();
			}
			currentSqlCommand.close();
		} catch (SQLException e) {
			logger.info("Failed query at player join. " + e.getMessage());
		}
	}
	
	@EventHandler
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		Vector lastLocation = event.getPlayer().getLocation().toVector();
		String sss = "UPDATE players SET `lastx`="+lastLocation.getBlockX()+", `lasty`="+lastLocation.getBlockY()+", `lastz`="+lastLocation.getBlockZ() + ", `lastworld`=\""+event.getPlayer().getLocation().getWorld().getUID().toString()+"\"" + " WHERE `uuid`=\""+event.getPlayer().getUniqueId()+"\"";
		doSQLUpdate(sss);
		//event.getPlayer().sendMessage("Set /back to " + lastLocation.getBlockX() + " " + lastLocation.getBlockY() + " " + lastLocation.getBlockZ());
	}
	
	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) throws SQLException {
		currentSqlCommand = c.createStatement();
		ResultSet s = currentSqlCommand.executeQuery("SELECT * FROM players WHERE `uuid`=\""+event.getPlayer().getUniqueId()+"\"");
		if(s.next()) {
			if (s.getBoolean("timber")) {
				int durability = recursiveWood(event.getBlock(), event);
				if (durability > 0)
					event.getPlayer().sendMessage("You just broke " + durability + " blocks.");
				event.getPlayer().getInventory().getItemInMainHand().setDurability((short)(event.getPlayer().getInventory().getItemInMainHand().getDurability()+(short)durability));
				if (event.getPlayer().getInventory().getItemInMainHand().getDurability() > event.getPlayer().getInventory().getItemInMainHand().getType().getMaxDurability())
					event.getPlayer().getInventory().remove(event.getPlayer().getInventory().getItemInMainHand());
			}
		}
		currentSqlCommand.close();
		currentSqlCommand = c.createStatement();
		ResultSet s2 = currentSqlCommand.executeQuery("SELECT * FROM players WHERE `uuid`=\""+event.getPlayer().getUniqueId()+"\"");
		if(s2.next()) {
			if (s2.getBoolean("ores")) {
				int ores = recursiveOre(event.getBlock(), event, event.getBlock().getType());
				event.getPlayer().getInventory().getItemInMainHand().setDurability((short)(event.getPlayer().getInventory().getItemInMainHand().getDurability()+(short)ores));
				if (event.getPlayer().getInventory().getItemInMainHand().getDurability() > event.getPlayer().getInventory().getItemInMainHand().getType().getMaxDurability())
					event.getPlayer().getInventory().remove(event.getPlayer().getInventory().getItemInMainHand());
				}
		}
		currentSqlCommand.close();
		currentSqlCommand = c.createStatement();
		
		Vector eventVec = event.getBlock().getLocation().toVector();
		Vector lookForVector = eventVec.add(new Vector(0,1,0));
		ResultSet rs = currentSqlCommand.executeQuery("SELECT * FROM players WHERE `homex`=" + lookForVector.getBlockX() + " AND `homey`=" + (lookForVector.getBlockY()) + " AND " + "`homez`=" + lookForVector.getBlockZ());
		if (rs.next()) {
			event.setCancelled(true);
		}
		currentSqlCommand.close();
	}
	
	private int recursiveWood(Block b, BlockBreakEvent e) {
		Vector originalVector = b.getLocation().toVector();
		int i = 0;
		Material f = e.getPlayer().getInventory().getItemInMainHand().getType();
		if (f == Material.DIAMOND_AXE || f == Material.GOLD_AXE || f == Material.IRON_AXE || f == Material.STONE_AXE || f == Material.WOOD_AXE)
			if (b.getType() == Material.LOG || b.getType() == Material.LOG_2 || b.getType() == Material.LEAVES|| b.getType() == Material.LEAVES_2) {
				getFortune(e);
				b.breakNaturally();
				i = getUnbreaking(e, i);
				i+=recursiveWood((originalVector.add(new Vector(1,0,0) )).toLocation(b.getWorld()).getBlock(), e);
				i+=recursiveWood((originalVector.add(new Vector(-2,0,0))).toLocation(b.getWorld()).getBlock(), e);
				i+=recursiveWood((originalVector.add(new Vector(1,1,0) )).toLocation(b.getWorld()).getBlock(), e);
				i+=recursiveWood((originalVector.add(new Vector(0,-2,0))).toLocation(b.getWorld()).getBlock(), e);
				i+=recursiveWood((originalVector.add(new Vector(0,1,1) )).toLocation(b.getWorld()).getBlock(), e);
				i+=recursiveWood((originalVector.add(new Vector(0,0,-2))).toLocation(b.getWorld()).getBlock(), e);
			}
		return i;
	}
	
	private int getUnbreaking(BlockBreakEvent e, int i) {
		int percent = 100;
		i+=1;
		int level = e.getPlayer().getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.DURABILITY);
		percent /= (level+1);
		if (rand.nextInt(100)+1 > percent)
			i-=1;
		return i;
	}
	
	private void getFortune(BlockBreakEvent e) {
		ItemStack item = e.getPlayer().getInventory().getItemInMainHand();
		int level = item.getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS);
		Material originalType = e.getBlock().getType();
		if (originalType == Material.IRON_ORE || originalType == Material.GOLD_ORE)
			return;
		int drops = 1;
		if (level == 1) {
			if (rand.nextInt(2) == 1)
				drops*=2;
		}
		if (level == 2) {
			int num = rand.nextInt(4);
			if (num == 1)
				drops*=2;
			else if (num==2)
				drops*=3;
		}
		if (level == 3) {
			int num2 = rand.nextInt(5);
			if (num2 == 0)
				drops*=2;
			if (num2 == 1) 
				drops*=3;
			if (num2 == 2)
				drops*=4;
		}
		for(int i = 0; i < drops-1; i++) {
			e.getBlock().setType(originalType);
			e.getBlock().breakNaturally();
		}
		e.getBlock().setType(originalType);
	}
	
	private int recursiveOre(Block b, BlockBreakEvent e, Material m) {
		Vector originalVector = b.getLocation().toVector();
		int i = 0;
		boolean special = false;
		if ((b.getType() == Material.REDSTONE_ORE && m == Material.GLOWING_REDSTONE_ORE) || (b.getType() == Material.GLOWING_REDSTONE_ORE && m == Material.REDSTONE_ORE))
			special = true;
		if (isPickAxe(e.getPlayer().getInventory().getItemInMainHand()) && isOre(m))
			if (b.getType() == m || special) {
				getFortune(e);
				b.breakNaturally();
				int level = getExperience(m);
				if (level > 0) {
					Location bLoc = b.getLocation();
					ExperienceOrb orb = bLoc.getWorld().spawn(bLoc, ExperienceOrb.class);
					orb.setExperience(level);
				}
				i = getUnbreaking(e, i);
				i+=recursiveOre((originalVector.add(new Vector(1,0,0) )).toLocation(b.getWorld()).getBlock(), e, m);
				i+=recursiveOre((originalVector.add(new Vector(-2,0,0))).toLocation(b.getWorld()).getBlock(), e, m);
				i+=recursiveOre((originalVector.add(new Vector(1,1,0) )).toLocation(b.getWorld()).getBlock(), e, m);
				i+=recursiveOre((originalVector.add(new Vector(0,-2,0))).toLocation(b.getWorld()).getBlock(), e, m);
				i+=recursiveOre((originalVector.add(new Vector(0,1,1) )).toLocation(b.getWorld()).getBlock(), e, m);
				i+=recursiveOre((originalVector.add(new Vector(0,0,-2))).toLocation(b.getWorld()).getBlock(), e, m);
			}
		return i;
	}
	
	private int getExperience(Material m) {
		switch(m) {
			case DIAMOND_ORE:
			case EMERALD_ORE:
				return rand.nextInt(5)+3;
			case COAL_ORE:
				return rand.nextInt(3);
			case LAPIS_ORE:
			case QUARTZ_ORE:
				return rand.nextInt(4)+2;
			case REDSTONE_ORE:
			case GLOWING_REDSTONE_ORE:
				return rand.nextInt(5)+1;
			default:
				return 0;
		}
	}
	
	private boolean isOre(Material m) {
		return m == Material.GLOWSTONE || m == Material.COAL_ORE || m == Material.DIAMOND_ORE || m == Material.EMERALD_ORE || m == Material.GLOWING_REDSTONE_ORE || m == Material.GOLD_ORE || m == Material.IRON_ORE || m == Material.LAPIS_ORE || m == Material.QUARTZ_ORE || m == Material.REDSTONE_ORE;
	}
	
	private boolean isPickAxe(ItemStack m) {
		Material f = m.getType();
		return f == Material.DIAMOND_PICKAXE || f == Material.GOLD_PICKAXE || f == Material.IRON_PICKAXE || f == Material.STONE_PICKAXE || f == Material.WOOD_PICKAXE;
	}
}
