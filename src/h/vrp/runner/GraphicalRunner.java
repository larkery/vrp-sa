package h.vrp.runner;

import h.util.random.HintonRandom;
import h.vrp.model.Instance;
import h.vrp.model.Solution;
import h.vrp.search.Annealer;
import h.vrp.search.EpochalGeometricTF;
import h.vrp.search.INeighbourhood;
import h.vrp.search.ITemperatureFunction;
import h.vrp.search.util.NeighbourhoodSpecParser;
import h.vrp.solcons.ISolutionConstructor;
import h.vrp.solcons.RandomConstructor;
import h.vrp.solcons.SavingsConstructor;
import h.vrp.sources.DatFile;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class GraphicalRunner {
	JFrame frame;
	Instance instance;
	private Solution solution;
	private HintonRandom random;
	private SolutionPanel solutionPanel;
	private AnnealerRun annealerRun;
	private Thread background;
	private JLabel ticks;
	public GraphicalRunner() {
		frame = new JFrame("VRP2 Test GUI");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		this.random = new HintonRandom(1);
		
		this.solution = null;
		this.instance = null;
		
		
		Container pane = new Container();
		
		frame.add(pane, BorderLayout.CENTER);
		GridBagLayout layout = new GridBagLayout();
		
		pane.setLayout(layout);
		
		this.solutionPanel = new SolutionPanel(instance, solution);
		GridBagConstraints constraints;
		constraints = new GridBagConstraints();
		constraints.gridy = 1;
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		constraints.fill = GridBagConstraints.BOTH;
		pane.add(solutionPanel, constraints);
		JButton button = new JButton("Load File");
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				//Create a file chooser
				final JFileChooser fc = new JFileChooser("/Users/hinton/Documents/PhD/thesis/localsearch/moves/experiments/DCVRP/");
				//In response to a button click:
				
				int returnVal = fc.showOpenDialog(frame);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					File file = fc.getSelectedFile();
					loadFile(file);
				}
			}
		});
		final ISolutionConstructor savingsConstructor = new SavingsConstructor();
		final ISolutionConstructor randomConstructor = new RandomConstructor(random, true, false);
		constraints = new GridBagConstraints();
		constraints.gridy = 0;
		constraints.gridx = 0;
		pane.add(button, constraints);
		
		button = new JButton("C&W");
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				createSolution(savingsConstructor);
			}
		});
		constraints.gridx = GridBagConstraints.RELATIVE;
		pane.add(button, constraints);
		
		button = new JButton("Randomise");
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				createSolution(randomConstructor);
			}
		});

		pane.add(button, constraints);
		
//		final JComboBox combo = new JComboBox(new String[] {
//				"Random", "Edge-Weighted", "CL+Random", "CL+EW"
//		});
		
		final JTextField nspec = new JTextField("(random)");
		GridBagConstraints gbn = new GridBagConstraints();
		gbn.gridx = GridBagConstraints.RELATIVE;
		
		gbn.fill=GridBagConstraints.BOTH;
		gbn.weightx = 2;
		pane.add(nspec, gbn);
//		constraints.fill = GridBagConstraints.NONE;
		
		final JButton annealButton = new JButton("Anneal");
		
		annealButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (annealButton.getText() == "Anneal") {
					String x = nspec.getText();
					startAnnealing(x);
					annealButton.setText("Stop");
				} else {
					annealButton.setText("Anneal");
					stopAnnealing();
				}
			}});
		
		pane.add(annealButton, constraints);
		
		ticks = new JLabel("0");
		pane.add(ticks, constraints);
	}
	
	class AnnealerRun implements Runnable {
		Annealer myAnnealer;
		boolean go;
		private JFrame frame;
		long ts;
		
		public AnnealerRun(Instance instance, Solution solution, INeighbourhood neighbourhood, JFrame frame) {
			
			myAnnealer = new Annealer(instance, solution, neighbourhood, 10, 0.9,1000, random);
			this.frame = frame;
		}
		@Override
		public void run() {
			ts = 0;
			go = true;
			
			Runnable repaint = new Runnable(){
				@Override
				public void run() {
					ticks.setText("" + ts);
					frame.repaint();
				}};
			while (go) {
				boolean accepted = myAnnealer.step();
				ts = myAnnealer.getTicks();
				if (accepted)
					System.out.println("gain: " + myAnnealer.lastDelta() + " (" + myAnnealer.getFitness() + ")");
//				System.err.println(neighbourhood);
				try {
					SwingUtilities.invokeAndWait(repaint);
				} catch (InterruptedException e) {
					
				} catch (InvocationTargetException e) {
					
				}
			}
		}
		public void kill() {
			go = false;
		}
	}
	
	protected void stopAnnealing() {
		if (annealerRun != null) {
			annealerRun.kill();
		}
//		try {
//			if (background != null) background.join();
//		} catch (InterruptedException e) {
//		}
		annealerRun = null;
		background = null;
	}

	protected void startAnnealing(String x) {
		INeighbourhood n = NeighbourhoodSpecParser.parseSexp(x, instance, solution, random);
		
		stopAnnealing();
		
		annealerRun = new AnnealerRun(instance, solution, n, frame);
		background = new Thread(annealerRun);
		background.start();
	}

	protected void createSolution(ISolutionConstructor constructor) {
		if (instance != null) {
			solution = constructor.createSolution(instance);
			solutionPanel.setSolution(solution);
			frame.repaint();
		}
	}

	protected void loadFile(File file) {
		try {
			DatFile datFile = new DatFile(file.getAbsolutePath());
			this.instance = datFile.createInstance();
			this.solution = null;
			instance.setInfeasibilityPenalty(1000);
			instance.setUsingHardConstraints(true);
			solutionPanel.setInstance(instance);
			solutionPanel.setSolution(solution);
			frame.repaint();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	
	
	public static void main(String[] args) throws FileNotFoundException {
		GraphicalRunner runner = new GraphicalRunner();
		runner.display();
	}
	private void display() {
		frame.pack();
		frame.setVisible(true);
	}
}
