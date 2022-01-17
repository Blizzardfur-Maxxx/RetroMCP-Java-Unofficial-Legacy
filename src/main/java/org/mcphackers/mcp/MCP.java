package org.mcphackers.mcp;

import jredfox.terminal.OpenTerminal;
import jredfox.terminal.app.TerminalApp;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.mcphackers.mcp.tasks.info.TaskInfo;

import java.util.*;

public class MCP extends TerminalApp {
	
	public static final String VERSION = "v0.1";
    public static EnumMode mode = null;
    public static EnumMode helpCommand = null;
    public static MCPLogger logger;
    public static Scanner input;
    private static final Ansi logo =
    		new Ansi()
            .fgCyan().a("  _____      _             ").fgYellow().a("__  __  _____ _____  ").a('\n')
            .fgCyan().a(" |  __ \\    | |           ").fgYellow().a("|  \\/  |/ ____|  __ \\ ").a('\n')
            .fgCyan().a(" | |__) |___| |_ _ __ ___ ").fgYellow().a("| \\  / | |    | |__) |").a('\n')
            .fgCyan().a(" |  _  // _ \\ __| '__/ _ \\").fgYellow().a("| |\\/| | |    |  ___/ ").a('\n')
            .fgCyan().a(" | | \\ \\  __/ |_| | | (_) ").fgYellow().a("| |  | | |____| |     ").a('\n')
            .fgCyan().a(" |_|  \\_\\___|\\__|_|  \\___/").fgYellow().a("|_|  |_|\\_____|_|     ").a('\n')
            .fgDefault();

    public MCP(Class<?> iclass, String[] args) {
        super(iclass, suggestAppId(iclass), "RetroMCP", VERSION, args);
    }

    public static void main(String[] args) {
        OpenTerminal.INSTANCE.run(new MCP(MCP.class, args));

    	AnsiConsole.systemInstall();
        logger = new MCPLogger();
        input = new Scanner(System.in);
        logger.log("Operating system: " + System.getProperty("os.name"));
        logger.log("RetroMCP " + VERSION);

        boolean startedWithNoParams = false;
        boolean exit = false;

        if (args.length <= 0) {
            startedWithNoParams = true;
            logger.println(logo);
            logger.println("RetroMCP " + VERSION);
            logger.println("Enter a command to execute:");
        }
        int executeTimes = 0;
        while (startedWithNoParams && !exit || !startedWithNoParams && executeTimes < 1) {
            while (args.length < 1) {
                logger.print(new Ansi().fgBrightCyan().a("> ").fgRgb(255,255,255));
                String str = "";
                try {
                	str = input.nextLine();
                } catch (NoSuchElementException ignored) {}
                logger.print(new Ansi().fgDefault());
                args = str.split(" ");
            }
            boolean taskMode = setMode(args[0]);
            Map<String, Object> parsedArgs = new HashMap<>();
            for (int index = 1; index < args.length; index++) {
                parseArg(args[index], parsedArgs);
            }
            setParams(parsedArgs, mode);
            if (taskMode) {
                start();
            } else if (mode == EnumMode.help) {
            	if(helpCommand == null) {
	                List<String[]> commands = new ArrayList<>();
	                for (EnumMode mode : EnumMode.values()) {
	                	commands.add(new String[]{mode.name(), mode.desc});
	            	}

                    for (String[] command : commands) {
                        for (int i2 = 0; i2 < command.length; i2++) {
                            if (i2 == 0)
                                logger.print(new Ansi().fgBrightMagenta().a(" - " + String.format("%-12s", command[i2])).fgDefault());
                            else
                                logger.print(new Ansi().fgGreen().a(" ").a(command[i2]).fgDefault());
                        }

                        logger.newLine();
                    }
            	}
            	else {
                    logger.println(new Ansi().fgBrightMagenta().a(" - " + String.format("%-12s", helpCommand.name())).fgDefault().fgGreen().a(" ").a(helpCommand.desc).fgDefault());
                    if(helpCommand.params.length > 0) logger.println("Optional parameters:");
                    for(String param : helpCommand.params) {
                		logger.println(new Ansi().a(" ").fgCyan().a(String.format("%-7s", param)).a(" - ").fgBrightYellow().a(EnumMode.getParamDesc(param)).fgDefault());
                    }
            	}
            } else if (mode != EnumMode.exit) {
                logger.println("Unknown command. Type 'help' for list of available commands");
            }
            args = new String[]{};
            MCPConfig.resetConfig();
            if (!startedWithNoParams || mode == EnumMode.exit)
                exit = true;
            mode = null;
            helpCommand = null;
            executeTimes++;
        }
        shutdown();
    }

    private static void setParams(Map<String, Object> parsedArgs, EnumMode mode) {
    	for (Map.Entry<String, Object> arg : parsedArgs.entrySet()) {
    		Object value = arg.getValue();
    		String name = arg.getKey();
    		if(value == null) {
    			switch (name) {
    			case "client":
                    case "server":
                        MCPConfig.setParameter(name, true);
        			break;
                }
        		if(mode == EnumMode.help) {
        			try {
        				helpCommand = EnumMode.valueOf(name);
        			}
        			catch (IllegalArgumentException ignored) {}
        		}
        		if(mode == EnumMode.setup) {
        			MCPConfig.setParameter("setupversion", name);
        		}
    		}
    		else if(value instanceof Integer) {
    			MCPConfig.setParameter(name, (Integer)value);
    		}
    		else if(value instanceof Boolean) {
    			MCPConfig.setParameter(name, (Boolean)value);
    		}
    		else if(value instanceof String) {
    			MCPConfig.setParameter(name, (String)value);
    		}
    		else if(value instanceof String[]) {
    			MCPConfig.setParameter(name, (String[])value);
    		}
    	}
	}

	private static void shutdown() {
        input.close();
        logger.close();
	}

	private static void start() {
        TaskInfo task = getTaskInfo(mode);
        try {
            logger.info(new Ansi().fgMagenta().a("====== ").fgDefault().a(task.title()).fgMagenta().a(" ======").fgDefault().toString());
            processTask(task);
            logger.resetProgressString();
            String completemsg = task.successMsg();
            if(completemsg != null) {
            	logger.info(new Ansi().a('\n').fgBrightGreen().a(completemsg).fgDefault().toString());
        		List<String> errors = task.getInfoList();
        		for(String error : errors) {
            		logger.info(" " + error.replace("\n", "\n "));
        		}
            }
        } catch (Exception e) {
        	logger.info(new Ansi().a('\n').fgBrightRed().a(task.failMsg()).fgDefault().toString());
    		List<String> errors = task.getInfoList();
    		for(String error : errors) {
        		logger.info(" " + error.replace("\n", "\n "));
    		}
            if (MCPConfig.debug) e.printStackTrace();
            else {
            	String msg = e.getMessage();
            	if(msg != null) {
            		logger.info(msg);
            	}
                logger.info("Use -debug for more info");
            }
        }
		task.clearInfoList();
    }

    public static TaskInfo getTaskInfo(EnumMode enumMode) {
        return enumMode.task;
    }

    private static void processTask(TaskInfo task) throws Exception {
    	if(task.isMultiThreaded()) {
    		processMultitasks(task);
    	}
    	else {
    		task.newTask(-1).doTask();
    	}
    }
    
    private static void processMultitasks(TaskInfo task) throws Exception {
        List<SideThread> threads = new ArrayList<>();
		if(MCPConfig.onlySide < 0 || MCPConfig.onlySide == SideThread.CLIENT) {
			threads.add(new SideThread(SideThread.CLIENT, task.newTask(SideThread.CLIENT)));
		}

		if(MCPConfig.onlySide < 0 || MCPConfig.onlySide == SideThread.SERVER) {
			threads.add(new SideThread(SideThread.SERVER, task.newTask(SideThread.SERVER)));
		}
        logger.newLine();
        for (SideThread thread : threads) {
            logger.newLine();
            thread.start();
        }
        boolean working = true;
        Exception ex = null;
        
        while (working) {
            Thread.sleep(10);
            logger.printProgressBars(threads);
            working = false;
            for(SideThread thread : threads) {
            	working = working || thread.isAlive();
            }
        }
        for(SideThread thread : threads) {
            if (thread.exception != null) {
            	ex = thread.exception;
            }
        }
    	if(ex != null) throw ex;
    }

    private static void parseArg(String arg, Map<String, Object> map) {
        int equalSign = arg.indexOf('=');
        if (arg.startsWith("-") && equalSign > 0) {
            String name = arg.substring(1, equalSign);
            String[] values = arg.substring(equalSign + 1).split(",");
            Object value;
            for (int i = 0; i < values.length; i++) {
            	values[i] = values[i].replace("\\n", "\n").replace("\\t", "\t");
            }
            if(values.length == 1) {
            	try {
                    value = Integer.parseInt(values[0]);
            	}
            	catch (NumberFormatException e) {
            		if(values[0].equals("false") || values[0].equals("true")) {
                        value = Boolean.parseBoolean(values[0]);
            		}
            		else {
            			value = values[0];
            		}
            	}
            }
            else {
            	value = values;
            }
            map.put(name, value);
        } else if (arg.startsWith("-")) {
            String name = arg.substring(1);
            map.put(name, true);
        } else {
            map.put(arg, null);
        }
    }

    private static boolean setMode(String name) {
    	try {
            mode = EnumMode.valueOf(name);
            return mode.task != null;
    	}
    	catch (IllegalArgumentException ignored) {}
        return false;
    }
}