package gmu.iit.gameboy;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashMap;

public class Joystick implements KeyListener {

	private enum Button {
		A, B, Start, Select, right, left, up, down;

		public int value;

		Button() { this.value = this.ordinal(); }
	}

	private short joystickState = 0xff;
	private MemoryMap _memoryMap;
	private Interrupts _interrupts;

	// Each keycode corresponds to a certain button for the gameboy
	private HashMap<Integer, Button> boundedKeys = new HashMap<Integer, Button>();

	/**
	 * Public constructor that takes in a MemoryMap
	 * @param memSet
	 */
	public Joystick(MemoryMap memSet, Interrupts interrupts) {
		_memoryMap = memSet;
		_interrupts = interrupts;

		boundedKeys.put(KeyEvent.VK_A, Button.A);
		boundedKeys.put(KeyEvent.VK_S, Button.B);
		boundedKeys.put(KeyEvent.VK_SPACE, Button.Start);
		boundedKeys.put(KeyEvent.VK_ENTER, Button.Select);
		boundedKeys.put(KeyEvent.VK_RIGHT, Button.right);
		boundedKeys.put(KeyEvent.VK_LEFT, Button.left);
		boundedKeys.put(KeyEvent.VK_UP, Button.up);
		boundedKeys.put(KeyEvent.VK_DOWN, Button.down);
	}

	/**
	 * Adds a binding of a key
	 * @param button
	 * @param keycode
	 */
	public void addKeyBinding(Button button, int keycode) {
		boundedKeys.put(keycode, button);
	}

	/**
	 * Removes a binding of a key
	 * @param button
	 * @param keycode
	 */
	public void removeKeyBinding(Button button, int keycode) {
		boundedKeys.remove(keycode);
	}

	/**
	 * Changes the binding of a key
	 * @param button
	 * @param oldKeyCode
	 * @param newKeyCode
	 */
	public void changeKeyBinding(Button button, int oldKeyCode, int newKeyCode) {
		boundedKeys.remove(oldKeyCode);
		boundedKeys.put(newKeyCode, button);
	}

	/**
	 * Should be used by the interrupt handler to get the joystick status.
	 * @return
	 */
	public short getJoypadState() {
		// int readStatus = (int)_memoryMap.readMemory(0xff00);
		int readStatus = _memoryMap.getIO()[0];
		readStatus ^= 0xff;

		if ((readStatus & 0x10) == 0) {
			readStatus &= (joystickState >> 4) | 0xf0;
		} else if ((readStatus & 0x20) == 0) {
			readStatus &= (joystickState & 0xf) | 0xf0;
		}

		return (short)readStatus;
	}

	/**
	 * Returns a string representation of the Joysticks that were pressed
	 * @return
	 */
	public String getJoystatusShort() {
		String val
		= String.format("Joystick status: %s %s %s %s, %s %s %s %s",
			(joystickState & 0x01) == 0  ? "A" : "_",
			(joystickState & 0x02) == 0  ? "B" : "_",
			(joystickState & 0x04) == 0  ? "Start" : "_",
			(joystickState & 0x08) == 0  ? "Select" : "_",
			(joystickState & 0x10) == 0  ? "Right" : "_",
			(joystickState & 0x20) == 0  ? "Left" : "_",
			(joystickState & 0x40) == 0  ? "Up" : "_",
			(joystickState & 0x80) == 0  ? "Down" : "_"
		);
		return val;
	}

	@Override
	public void keyTyped(KeyEvent e) { }

	@Override
	public void keyPressed(KeyEvent e) {
		if (boundedKeys.containsKey(e.getKeyCode())) {
			boolean previouslyUnset = false;

			int currentButton = boundedKeys.get(e.getKeyCode()).value;
			if ((joystickState & (1 << currentButton)) == 0) {
				previouslyUnset = true;
			}
			joystickState &= (0xff & (~(1 << currentButton)));

			int keyReq = (int)_memoryMap.readMemory(0xff00);

			boolean requestInterrupt =
				((currentButton > 3)  && ((keyReq & 0x10) == 0)) ||
				((currentButton <= 3) && ((keyReq & 0x20) == 0));

			if (requestInterrupt && !previouslyUnset) {
				// System.out.println(getJoystatusShort());
				_interrupts.requestInterrupt(Interrupts.InterruptTypes.P10_13);
			}

			_memoryMap.writeMemory(0xff00, (char)joystickState);
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		if (boundedKeys.containsKey(e.getKeyCode())) {
			joystickState |= 1 << (boundedKeys.get(e.getKeyCode()).value);
			// System.out.println(getJoystatusShort());
		}

		_memoryMap.writeMemory(0xff00, (char)joystickState);
	}
}
