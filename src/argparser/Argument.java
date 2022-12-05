package argparser;

import argparser.utils.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class Argument<Type extends ArgumentType<TInner>, TInner>
	implements MinimumErrorLevelConfig<CustomError>, ErrorCallbacks<TInner, Argument<Type, TInner>>
{
	final Type argType;
	private char prefix = '-';
	private final List<String> names = new ArrayList<>();
	private short usageCount = 0;
	private boolean obligatory = false, positional = false, allowUnique = false;
	private TInner defaultValue;
	private Command parentCmd;
	private Consumer<Argument<Type, TInner>> onErrorCallback;
	private Consumer<TInner> onCorrectCallback;


	public Argument(Type argType, String... names) {
		this.addNames(names);
		this.argType = argType;
	}

	public Argument(String name, Type argType) {
		this(argType, name);
	}

	public Argument(String[] name, Type argType) {
		this(argType, name);
	}

	public Argument(char name, Type argType) {
		this(argType, String.valueOf(name));
	}

	public Argument(char charName, String fullName, Type argType) {
		this(argType, String.valueOf(charName), fullName);
	}

	/**
	 * Creates an argument of type {@link argparser.argumentTypes.BooleanArgument} with the given name.
	 */
	@SuppressWarnings("unchecked cast") // we know for sure type returned by BOOLEAN is compatible
	public Argument(String name) {this(name, (Type)ArgumentType.BOOLEAN());}


	/**
	 * Marks the argument as obligatory. This means that this argument should <b>always</b> be used
	 * by the user.
	 */
	public Argument<Type, TInner> obligatory() {
		this.obligatory = true;
		return this;
	}

	public boolean isObligatory() {
		return obligatory;
	}

	/**
	 * Marks the argument as positional. This means that the value of this argument may be specified directly
	 * without indicating the name/name of this argument. The positional place where it should be placed is
	 * defined by the order of creation of the argument definitions.
	 * <li>Note that an argument marked as positional can still be used by specifying its name/name.
	 */
	public Argument<Type, TInner> positional() {
		if (this.getNumberOfValues().max == 0) {
			throw new IllegalArgumentException("An argument that does not accept values cannot be positional");
		}
		this.positional = true;
		return this;
	}

	public boolean isPositional() {
		return positional;
	}

	/**
	 * Specify the prefix of this argument. By default, this is <code>'-'</code>. If this argument is used in an
	 * argument name list (-abc), the prefix that will be valid is any against all the arguments specified
	 * in that name list.
	 */
	public Argument<Type, TInner> prefix(char prefix) {
		this.prefix = prefix;
		return this;
	}

	public char getPrefix() {
		return prefix;
	}

	/**
	 * Specifies that this argument has priority over other arguments, even if they are obligatory.
	 * This means that if an argument in a command is set as obligatory, but one argument with {@link #allowUnique}
	 * was used, then the unused obligatory argument will not throw an error.
	 */
	public Argument<Type, TInner> allowUnique() {
		this.allowUnique = true;
		return this;
	}

	public boolean allowsUnique() {
		return allowUnique;
	}

	/**
	 * The value that should be used if the user does not specify a value for this argument. If the argument
	 * does not accept values, this value will be ignored.
	 */
	public Argument<Type, TInner> defaultValue(TInner value) {
		this.defaultValue = Objects.requireNonNull(value);
		return this;
	}

	/**
	 * Add more names to this argument. This is useful if you want the same argument to be used with multiple
	 * different names.
	 */
	public Argument<Type, TInner> addNames(String... names) {
		Objects.requireNonNull(names);

		Arrays.stream(names)
			.map(UtlString::sanitizeName)
			.forEach(newName -> {
				if (this.names.contains(newName)) {
					throw new IllegalArgumentException("Name '" + newName + "' is already used by this argument.");
				}
				this.names.add(newName);
			});
		return this;
	}

	public boolean hasName(String name) {
		return this.names.contains(name);
	}

	public List<String> getNames() {
		return names;
	}

	public String getDisplayName() {
		return this.names.get(0);
	}

	public ArgValueCount getNumberOfValues() {
		return this.argType.getNumberOfArgValues();
	}

	void setParentCmd(Command parentCmd) {
		if (this.parentCmd != null) {
			throw new IllegalStateException("Argument already added to a command");
		}
		this.parentCmd = parentCmd;
	}

	Command getParentCmd() {
		return parentCmd;
	}

	public short getUsageCount() {
		return usageCount;
	}

	/**
	 * Specify a function that will be called with the value introduced by the user.
	 */
	public Argument<Type, TInner> onOk(Consumer<TInner> callback) {
		this.setOnCorrectCallback(callback);
		return this;
	}

	/**
	 * Specify a function that will be called if an error occurs when parsing this argument.
	 */
	public Argument<Type, TInner> onErr(Consumer<Argument<Type, TInner>> callback) {
		this.setOnErrorCallback(callback);
		return this;
	}

	/**
	 * Pass the specified values array to the argument type to parse it.
	 * @param tokenIndex This is the global index of the token that is currently being parsed. Used when
	 * dispatching errors.
	 */
	void parseValues(String[] value, short tokenIndex) {
		this.argType.setTokenIndex(tokenIndex);
		this.argType.parseArgumentValues(value);
		this.usageCount++;
	}

	/**
	 * {@link #parseValues(String[], short)} but passes in an empty values array to parse.
	 */
	void parseValues() {
		this.parseValues(new String[0], (short)0);
	}

	/**
	 * Returns the final parsed value of this argument.
	 * @param parseState The current state of the parser. Used to dispatch any possible errors.
	 */
	TInner finishParsing(Command.ParsingState parseState) {
		if (this.usageCount == 0) {
			if (this.obligatory && !this.parentCmd.uniqueArgumentReceivedValue()) {
				parseState.addError(ParseError.ParseErrorType.OBLIGATORY_ARGUMENT_NOT_USED, this, 0);
				return null;
			}
			return this.defaultValue;
		}

		this.argType.getErrorsUnderDisplayLevel().forEach(parseState::addError);
		return this.argType.getFinalValue();
	}

	/**
	 * Checks if this argument matches the given name, including the prefix.
	 */
	boolean checkMatch(String name) {
		return this.names.stream().anyMatch(a -> name.equals(Character.toString(this.prefix).repeat(2) + a));
	}

	/**
	 * Checks if this argument matches the given single character name.
	 */
	boolean checkMatch(char name) {
		return this.hasName(Character.toString(name));
	}

	// no worries about casting here, it will always receive the correct type
	@SuppressWarnings("unchecked")
	void invokeCallbacks(Object okValue) {
		this.invokeCallbacks();
		if (
			this.onCorrectCallback == null
				|| this.usageCount == 0
				|| (!this.allowUnique && this.parentCmd.uniqueArgumentReceivedValue())
		) return;

		this.onCorrectCallback.accept((TInner)okValue);
	}

	public boolean equals(Argument<?, ?> obj) {
		// we just want to check if there's a difference between identifiers and both are part of the same command
		return this.parentCmd == obj.parentCmd && (
			this.getNames().stream().anyMatch(name -> obj.getNames().contains(name))
		);
	}

	@Override
	public String toString() {
		return String.format(
			"Argument<%s>[names=%s, prefix='%c', obligatory=%b, positional=%b, allowUnique=%b, defaultValue=%s]",
			this.argType.getClass().getSimpleName(), this.names, this.prefix, this.obligatory,
			this.positional, this.allowUnique, this.defaultValue
		);
	}


	// ------------------------------------------------ Error Handling ------------------------------------------------
	// just act as a proxy to the type error handling

	@Override
	public List<CustomError> getErrorsUnderExitLevel() {
		return this.argType.getErrorsUnderExitLevel();
	}

	@Override
	public List<CustomError> getErrorsUnderDisplayLevel() {
		return this.argType.getErrorsUnderDisplayLevel();
	}

	@Override
	public boolean hasExitErrors() {
		return this.argType.hasExitErrors() || !this.getErrorsUnderExitLevel().isEmpty();
	}

	@Override
	public boolean hasDisplayErrors() {
		return this.argType.hasDisplayErrors() || !this.getErrorsUnderDisplayLevel().isEmpty();
	}

	@Override
	public void setMinimumDisplayErrorLevel(ErrorLevel level) {
		this.argType.setMinimumDisplayErrorLevel(level);
	}

	@Override
	public ModifyRecord<ErrorLevel> getMinimumDisplayErrorLevel() {
		return this.argType.getMinimumDisplayErrorLevel();
	}

	@Override
	public void setMinimumExitErrorLevel(ErrorLevel level) {
		this.argType.setMinimumExitErrorLevel(level);
	}

	@Override
	public ModifyRecord<ErrorLevel> getMinimumExitErrorLevel() {
		return this.argType.getMinimumExitErrorLevel();
	}

	@Override
	public void setOnErrorCallback(Consumer<Argument<Type, TInner>> callback) {
		this.onErrorCallback = callback;
	}

	@Override
	public void setOnCorrectCallback(Consumer<TInner> callback) {
		this.onCorrectCallback = callback;
	}

	@Override
	public void invokeCallbacks() {
		if (this.onErrorCallback == null || this.hasExitErrors()) return;
		this.onErrorCallback.accept(this);
	}
}


interface ArgumentAdder {
	/**
	 * Inserts an argument for this command to be parsed.
	 *
	 * @param argument the argument to be inserted
	 * @param <T> the ArgumentType subclass that will parse the value passed to the argument
	 * @param <TInner> the actual type of the value passed to the argument
	 */
	<T extends ArgumentType<TInner>, TInner> void addArgument(Argument<T, TInner> argument);
}