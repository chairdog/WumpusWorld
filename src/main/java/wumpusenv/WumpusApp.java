package wumpusenv;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.CardLayout;
import java.awt.Dialog;
import java.awt.Event;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Label;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Properties;

import javax.swing.SwingUtilities;

import eis.iilang.EnvironmentState;

/**
 * WumpusApp is a combination of editor and runner for the agent in the Wumpus
 * World. It is called from the WumpusWorld class. It contains TWO PANELS: the
 * Runner and the WorldEditor.
 */

public class WumpusApp extends Frame {

	private static final long serialVersionUID = 8714859195624414371L;

	// World editor
	private WorldEditor worldEditor;
	private Menu worldEditorMenu;
	public static String WORLDED = "World Editor";

	// Runner
	private Runner runner;
	// private Menu runnerMenu;
	public static String RUNNER = "Wumpus Runner";

	// Graphics
	private static String fPath = "wumpusenv/images";
	private CardLayout cardLayout;
	private Panel mainPanel;
	private Properties preferences;
	private Dialog errorDialog = new Dialog(this, "Error!", true);
	private Label errorLabel = new Label("");
	// private CheckboxMenuItem toggleDebug, toggleGC, toggleSIM;

	/** observer, to be called when state channge happens */
	WumpusWorld myObserver;

	private boolean guiVisible;

	/**
	 * constructor. Takes the "parent" as observer, to call it back when a state
	 * change happens. State changes are directly coupled to the selected panel.
	 * If the panel is the World Editor, the mode is PAUSED, and when the panel
	 * is the Runner, the mode is RUNNING.
	 * 
	 * @param showGui
	 *            true if GUI should be shown, false if not.
	 */
	public WumpusApp(WumpusWorld obs, boolean showGui) {
		super("Wumpus environment editor and simulator");
		guiVisible = showGui;
		InitWumpusApp();
		myObserver = obs;
	}

	/**
	 * init the frame, set window position
	 */
	public void InitWumpusApp() {

		worldEditor = new WorldEditor(this);
		runner = new Runner(this);
		preferences = loadPrefs();

		if (isGuiVisible()) {
			setLayout(new BorderLayout());

			try {
				setIconImage(getToolkit().getImage(fPath + "/wumpus.gif"));
			} catch (Exception e) {
				e.printStackTrace();
			}
			cardLayout = new CardLayout();

			mainPanel = new Panel();
			mainPanel.setLayout(cardLayout);
			mainPanel.add(WORLDED, worldEditor);
			mainPanel.add(RUNNER, runner);
			Panel buttonPanel = new Panel();
			buttonPanel.setLayout(new GridLayout(1, 3));
			buttonPanel.add(new Button(WORLDED));
			buttonPanel.add(new Button(RUNNER));
			add("Center", mainPanel);
			add("South", buttonPanel);
			setMenuBar(setupMenuBar());

			setSize(WumpusSettings.getWidth(), WumpusSettings.getHeight());
			setLocation(WumpusSettings.getX(), WumpusSettings.getY());
			setVisible(true);
			addWindowsListeners();
		}
	}

	private void addWindowsListeners() {
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentMoved(ComponentEvent e) {
				saveWindowSettings();
			}

			@Override
			public void componentResized(ComponentEvent e) {
				saveWindowSettings();
			}
		});
	}

	/**
	 * DOC
	 * 
	 * @return
	 */
	public Runner getRunner() {
		return runner;
	}

	/**
	 * DOC
	 * 
	 * @return
	 */
	private MenuBar setupMenuBar() {
		MenuBar menuBar = new MenuBar();
		// Menu fileMenu = new Menu("File");
		// fileMenu.add("Quit"); // disabled

		worldEditorMenu = new Menu("World Editor");
		worldEditorMenu.add("Load world");
		worldEditorMenu.add("Save world");

		// menuBar.add(fileMenu);
		menuBar.add(worldEditorMenu);

		return menuBar;
	}

	/**
	 * DOC
	 * 
	 * @return
	 */
	private Properties loadPrefs() {
		return new Properties();
	}

	/**
	 * DOC
	 * 
	 * @param pName
	 * @return
	 */
	public Image getImage(String pName) {
		try {
			java.net.URL u = getClass().getClassLoader().getResource(
					fPath + "/" + pName);
			return getToolkit().getImage(u);
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	/**
	 * Closes down the Wumpus environment.
	 * 
	 * First unregisters entity with EIS, if needed.
	 * 
	 * @param evt
	 */
	@Override
	public boolean handleEvent(Event evt) {
		switch (evt.id) {
		case Event.WINDOW_DESTROY:
			// clean up first
			WumpusWorld.getInstance().unregisterEntity();
			// close
			WumpusWorld.getInstance().close();
			return true;
		default:
			return super.handleEvent(evt);
		}
	}

	/**
	 * Executes action contained in object.
	 * 
	 * @param event
	 *            is ignored I think.
	 * @param obj
	 *            is WORLDED, RUNNER, "Load world" or ""Save World".
	 * @return {@code true} if successful; {@code false} if command failed.
	 */
	public boolean action(Event event, Object obj) {
		if (obj == WORLDED) {
			cardLayout.show(mainPanel, WORLDED);
			notifyObservers(EnvironmentState.PAUSED);
			return true;
		}
		if (obj == RUNNER) {
			cardLayout.show(mainPanel, RUNNER);
			runner.setRealModel(worldEditor.getModel());
			if (!worldEditor.getModel().gameFinished()) {
				notifyObservers(EnvironmentState.RUNNING);
			}
			return true;
		}
		if ("Load world".equals(obj)) {
			FileDialog fd = new FileDialog(this, "Load a world",
					FileDialog.LOAD);
			FilenameFilter fnf = new ExtensionFilter(".wld");
			fd.setFilenameFilter(fnf);
			fd.setDirectory(preferences.getProperty("homedir"));
			fd.setFile("*.wld");
			fd.show();
			if (fd.getFile() != null) {
				worldEditor
						.loadFrom(new File(fd.getDirectory() + fd.getFile()));
			}
			cardLayout.show(mainPanel, WORLDED);
			System.out.println("New world loaded");
			runner.reset();
			System.out.println("Runner reset");
			return true;
		}
		if ("Save world".equals(obj)) {
			FileDialog fd = new FileDialog(this, "Save a world",
					FileDialog.SAVE);
			FilenameFilter fnf = new ExtensionFilter(".wld");
			fd.setFilenameFilter(fnf);
			fd.setDirectory(preferences.getProperty("homedir"));
			fd.setFile("*.wld");
			fd.show();
			if (fd.getFile() != null) {
				worldEditor.saveTo(new File(fd.getDirectory() + fd.getFile()));
			}
			return true;
		}

		return false;
	}

	/**
	 * DOC
	 * 
	 * @param msg
	 */
	public void reportError(String msg) {
		errorLabel.setText(msg);
		errorDialog.pack();
		errorDialog
				.setLocation(
						getLocation().x
								+ (getSize().width - errorDialog.getSize().width)
								/ 2,
						getLocation().y
								+ (getSize().height - errorDialog.getSize().height)
								/ 2);
		errorDialog.show();
	}

	/**
	 * 
	 * @return
	 */
	public WorldEditor getEditor() {
		return worldEditor;
	}

	/**
	 * Notifies observers that the state of the Wumpus environment has changed.
	 */
	public void notifyObservers(EnvironmentState state) {
		myObserver.notifyStateChange(state);
	}

	/**
	 * Closes the window.
	 */
	public void close() {
		if (isGuiVisible()) {
			saveWindowSettings();
		}
		closeWindows();
	}

	/**
	 * Close our window. Effect will be later as this uses
	 * SwingUtilities.invokeLater.
	 * 
	 * @param app
	 */
	protected void closeWindows() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				dispose();
				setVisible(false);
			}
		});
	}

	private void saveWindowSettings() {
		WumpusSettings.setWindowParams(getX(), getY(), getWidth(), getHeight());
	}

	/**
	 * check if GUI should be visible.
	 * 
	 * @return true if GUI is visible, false if GUI should not be rendered.
	 */
	public boolean isGuiVisible() {
		return guiVisible;
	}
}

/**
 * DOC
 */
class ExtensionFilter implements FilenameFilter {
	private String extension;

	/**
	 * DOC
	 * 
	 * @param extension
	 */
	public ExtensionFilter(String extension) {
		this.extension = extension;
	}

	/**
	 * DOC
	 */
	public boolean accept(File dir, String name) {
		return (name.indexOf(extension) != -1);
	}

}

/**
 * DOC
 */
class PrefDialog extends Dialog {

	private static final long serialVersionUID = 4093273691750133525L;

	protected TextField homeDir = new TextField();

	/**
	 * DOC
	 * 
	 * @param owner
	 */
	public PrefDialog(Frame owner) {
		super(owner, "Preferences", true);
		setLayout(new BorderLayout());
		Panel prefs = new Panel();
		prefs.setLayout(new FlowLayout());
		prefs.add(new Label("Home directory"));
		prefs.add(homeDir);
	}

}
