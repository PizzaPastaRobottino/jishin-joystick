package joystick;

import joystick.network.SocketThread;
import joystick.shared.JoystickState;
import net.java.games.input.Component;
import net.java.games.input.Component.Identifier;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Joystick Test with JInput
 *
 * @author TheUzo007
 *         http://theuzo007.wordpress.com
 *         <p>
 *         Created 22 Oct 2013
 *         <p>
 *         Analog sx => sterzo y axis
 *         Acceleratore R2 ry
 *         X => shift up 1
 *         quadrato => shift down 0
 *         Retro => marcia -1
 */
public class JoystickTest {
    private final SocketThread socketThread;
    private JoystickState lastState = new JoystickState(false, false, 0f, 0f);

    public static void main(String args[]) {
        new JoystickTest();
    }

    final JFrameWindow window;
    private ArrayList<Controller> foundControllers;

    public JoystickTest() {
        window = new JFrameWindow();
        socketThread = window.getSocketThread();

        foundControllers = new ArrayList<>();
        System.out.println(foundControllers);
        searchForControllers();

        // If at least one controller was found we start showing controller data on window.
        if (!foundControllers.isEmpty())
            startShowingControllerData();
        else
            window.addControllerName("No controller found!");
    }

    /**
     * Search (and save) for controllers of type Controller.Type.STICK,
     * Controller.Type.GAMEPAD, Controller.Type.WHEEL and Controller.Type.FINGERSTICK.
     */
    private void searchForControllers() {
        Controller[] controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();

        for (int i = 0; i < controllers.length; i++) {
            Controller controller = controllers[i];

            if (
                    controller.getType() == Controller.Type.STICK ||
                            controller.getType() == Controller.Type.GAMEPAD ||
                            controller.getType() == Controller.Type.WHEEL ||
                            controller.getType() == Controller.Type.FINGERSTICK
                    ) {
                // Add new controller to the list of all controllers.
                foundControllers.add(controller);

                // Add new controller to the list on the window.
                window.addControllerName(controller.getName() + " - " + controller.getType().toString() + " type");
            }
        }
    }

    /**
     * Starts showing controller data on the window.
     */
    private void startShowingControllerData() {
        while (true) {
            JoystickState currentState = new JoystickState();

            // Currently selected controller.
            int selectedControllerIndex = window.getSelectedControllerName();
            Controller controller = foundControllers.get(selectedControllerIndex);

            // Pull controller for current data, and break while loop if controller is disconnected.
            if (!controller.poll()) {
                window.showControllerDisconnected();
                break;
            }

            // X axis and Y axis
            int xAxisPercentage = 0;
            int yAxisPercentage = 0;
            // JPanel for other axes.
            JPanel axesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 25, 2));
            axesPanel.setBounds(0, 0, 200, 190);

            // JPanel for controller buttons
            JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 1));
            buttonsPanel.setBounds(6, 19, 246, 110);

            // Go trough all components of the controller.
            Component[] components = controller.getComponents();
            for (int i = 0; i < components.length; i++) {
                Component component = components[i];
                Identifier componentIdentifier = component.getIdentifier();

                // Buttons
                if (componentIdentifier.getName().matches("^[0-9]*$")) { // If the component identifier name contains only numbers, then this is a button.
                    // Is button pressed?
                    boolean isItPressed = true;
                    if (component.getPollData() == 0.0f)
                        isItPressed = false;

                    if (componentIdentifier.getName().equals("1") && isItPressed) // 3
                        currentState.setShiftDown(true);

                    if (componentIdentifier.getName().equals("0") && isItPressed) // 2
                        currentState.setShiftUp(true);

                    // Button index
                    String buttonIndex;
                    buttonIndex = component.getIdentifier().toString();

                    // Create and add new button to panel.
                    JToggleButton aToggleButton = new JToggleButton(buttonIndex, isItPressed);
                    aToggleButton.setPreferredSize(new Dimension(48, 25));
                    aToggleButton.setEnabled(isItPressed);
                    buttonsPanel.add(aToggleButton);

                    // We know that this component was button so we can skip to next component.
                    continue;
                }

                // Hat switch
                if (componentIdentifier == Component.Identifier.Axis.POV) {
                    float hatSwitchPosition = component.getPollData();
                    window.setHatSwitch(hatSwitchPosition);

                    // We know that this component was hat switch so we can skip to next component.
                    continue;
                }

                // Axes
                if (component.isAnalog()) {
                    float axisValue = component.getPollData();
                    int axisValueInPercentage = getAxisValueInPercentage(axisValue);

                    // X axis
                    if (componentIdentifier == Component.Identifier.Axis.X) {
                        xAxisPercentage = axisValueInPercentage;
                        currentState.setSterzo(axisValueInPercentage / 50f - 1f);
                        continue; // Go to next component.
                    }
                    // Y axis
                    if (componentIdentifier == Component.Identifier.Axis.Y) {
                        yAxisPercentage = axisValueInPercentage;
                        continue; // Go to next component.
                    }

                    if (component.getName().equals("ry")) {
                        currentState.setAcceleratore(axisValueInPercentage / 0.86f);
                        //currentState.setAcceleratore(-((axisValueInPercentage - 50f) / 50f)); // Secondo joystick
                    }

                    // Other axis
                    JLabel progressBarLabel = new JLabel(component.getName());
                    JProgressBar progressBar = new JProgressBar(0, 100);
                    progressBar.setValue(axisValueInPercentage);
                    axesPanel.add(progressBarLabel);
                    axesPanel.add(progressBar);
                }
            }

            // Now that we go trough all controller components,
            // we add butons panel to window,
            window.setControllerButtons(buttonsPanel);
            // set x and y axes,
            window.setXYAxis(xAxisPercentage, yAxisPercentage);
            // add other axes panel to window.
            window.addAxisPanel(axesPanel);

            try {
                socketThread.sendState(lastState.difference(currentState));
            } catch (IOException e) {
                System.err.println(e.getMessage());
                break;
            }

            lastState = currentState;

            // We have to give processor some rest.
            try {
                Thread.sleep(50);
                //Thread.sleep(250);
            } catch (InterruptedException ex) {
                Logger.getLogger(JoystickTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Given value of axis in percentage.
     * Percentages increases from left/top to right/bottom.
     * If idle (in center) returns 50, if joystick axis is pushed to the left/top
     * edge returns 0 and if it's pushed to the right/bottom returns 100.
     *
     * @return value of axis in percentage.
     */
    private int getAxisValueInPercentage(float axisValue) {
        return (int) (((2 - (1 - axisValue)) * 100) / 2);
    }
}
