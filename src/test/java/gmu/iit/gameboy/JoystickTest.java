package gmu.iit.gameboy;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.JFrame;

import org.junit.Test;

public class JoystickTest {

    @Test
    public void joystickTester() throws FileNotFoundException, IOException {
        RegisterSet registerSet = new RegisterSet();
        Joystick joystick = new Joystick(new MemoryMap(registerSet), null);

        // Create and set up the window.
        JFrame frame = new JFrame("Joystick debugging");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.addKeyListener(joystick);

        // Display the window.
        frame.pack();
        frame.setVisible(true);
    }
}
