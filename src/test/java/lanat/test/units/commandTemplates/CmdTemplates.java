package lanat.test.units.commandTemplates;

import lanat.Argument;
import lanat.Command;
import lanat.CommandTemplate;
import lanat.argumentTypes.BooleanArgumentType;
import lanat.argumentTypes.FloatArgumentType;
import lanat.argumentTypes.IntArgumentType;
import lanat.argumentTypes.StringArgumentType;

import java.util.Optional;

public class CmdTemplates {
	@Command.Define(names = "cmd1")
	public static class CmdTemplate1 extends CommandTemplate {
		@Argument.Define(argType = IntArgumentType.class)
		public Integer number;

		@Argument.Define(argType = StringArgumentType.class)
		public String text;

		@Argument.Define(names = { "name1", "f" }, argType = BooleanArgumentType.class)
		public boolean flag;

		@Argument.Define(argType = IntArgumentType.class)
		public Optional<Integer> numberParsedArgValue = Optional.of(0);


		@CommandAccessor
		public CmdTemplate1_1 cmd2;

		@Command.Define(names = "cmd1-1")
		public static class CmdTemplate1_1 extends CommandTemplate {
			@Argument.Define(argType = FloatArgumentType.class)
			public Float number;

			@Argument.Define(argType = IntArgumentType.class)
			public Optional<Integer> number2;
		}
	}

	@Command.Define(names = "cmd2")
	public static class CmdTemplate2 extends CommandTemplate {
		@Command.Define
		public static class CmdTemplate2_1 extends CommandTemplate { }
	}

	@Command.Define
	public static class CmdTemplate3 extends CommandTemplate {
		@Argument.Define(argType = IntArgumentType.class, positional = true)
		public int number;

		@CommandAccessor
		public CmdTemplate3_1 cmd2;

		@Command.Define(names = "cmd3-1")
		public static class CmdTemplate3_1 extends CommandTemplate {
			@Argument.Define(argType = IntArgumentType.class, positional = true)
			public int number;

			@CommandAccessor
			public CmdTemplate3_1_1 cmd3;

			@Command.Define(names = "cmd3-1-1")
			public static class CmdTemplate3_1_1 extends CommandTemplate {
				@Argument.Define(argType = IntArgumentType.class, positional = true)
				public int number;
			}
		}
	}
}
