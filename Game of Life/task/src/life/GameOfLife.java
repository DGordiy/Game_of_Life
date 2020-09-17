package life;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.io.IOException;
import java.util.Random;

class Population {
    private final int size;
    private boolean[][] universe;
    private int alive;
    private int generation;

    public Population(int size) {
        this.size = size;
        universe = new boolean[size][size];
    }

    public int getGeneration() {
        return generation;
    }

    public int getAlive() {
        return alive;
    }

    public boolean[][] getUniverse() {
        return universe.clone();
    }

    public void generate() {
        alive = 0;
        generation = 0;
        Random random = new Random();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                universe[i][j] = random.nextBoolean();
                if (universe[i][j]) {
                    alive++;
                }
            }
        }
    }

    public void printUniverse() {
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                Runtime.getRuntime().exec("clear");
            }
        } catch (IOException | InterruptedException e) {}

        System.out.println("Generation #" + generation);
        System.out.println("Alive: " + alive);
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                System.out.print(universe[i][j] ? 'O' : ' ');
            }
            System.out.println();
        }
    }

    public void makeNewGeneration() {
        boolean[][] newGeneration = new boolean[size][size];

        alive = 0;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                int neighbours = neighbours(i, j);

                if (universe[i][j]) {
                    newGeneration[i][j] = neighbours == 2 || neighbours == 3;
                } else {
                    newGeneration[i][j] = neighbours == 3;
                }

                if (newGeneration[i][j]) {
                    alive++;
                }
            }
        }

        universe = newGeneration;
        generation++;
    }

    private int neighbours(int row, int col) {
        int i1;
        int j1;
        int count = 0;

        for (int i = row - 1; i < row + 2; i++) {
            for (int j = col - 1; j < col + 2; j++) {
                if (i == row && j == col) {
                    continue;
                }

                if (i < 0) {
                    i1 = size - 1;
                } else if (i == size) {
                    i1 = 0;
                } else if (i > size) {
                    i1 = 1;
                } else {
                    i1 = i;
                }
                if (j < 0) {
                    j1 = size - 1;
                } else if (j == size) {
                    j1 = 0;
                } else if (j > size) {
                    j1 = 1;
                } else {
                    j1 = j;
                }

                if (universe[i1][j1]) {
                    count++;
                }
            }
        }

        return count;
    }
}

class Game extends Thread {
    private final GameOfLife gol;

    Game(GameOfLife gol) {
        this.gol = gol;
    }

    @Override
    public void run() {
        super.run();

        Population population = new Population(GameOfLife.SIZE);
        population.generate();
        gol.setPopulation(population);

        while (!isInterrupted()) {
            if (!gol.isPause()) {
                try {
                    Thread.sleep(500 / gol.getSpeed());
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                }

                population.makeNewGeneration();
                gol.updateGenerationInfo();
            }
        }
    }
}

public class GameOfLife extends JFrame {
    public final static int SIZE = 60;

    private Population population;

    private final JLabel generationLabel;
    private final JLabel aliveLabel;
    private final JPanel universePanel;
    private final JToggleButton pauseBtn;

    private boolean pause = false;
    private int speed = 5;
    private Game currentGame;

    public GameOfLife() {
        super("Game of Life");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        int WIDTH = 620;
        int HEIGHT = 495;
        setSize(WIDTH, HEIGHT);
        setResizable(false);

        JPanel leftPanel = new JPanel();
        //leftPanel.setBounds(0, 60, 140, 200);
        leftPanel.setBounds(0, 0, 140, 200);
        leftPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        GridLayout gridLayout = new GridLayout(6, 1);
        leftPanel.setLayout(gridLayout);

        pauseBtn = new JToggleButton();
        pauseBtn.setName("PlayToggleButton");
        pauseBtn.setText("Pause");
        pauseBtn.addActionListener(actionEvent -> {
            pause = !pause;
            if (pause) {
                pauseBtn.setText("Resume");
            } else {
                pauseBtn.setText("Pause");
            }
        });
        //pauseBtn.setBounds(0, 0, 100, 30);
        leftPanel.add(pauseBtn);

        JButton resetBtn = new JButton();
        resetBtn.setName("ResetButton");
        resetBtn.setText("Reset");
        resetBtn.addActionListener(actionEvent -> {
            currentGame.interrupt();
            currentGame = new Game(this);
            currentGame.start();
        });
        //resetBtn.setBounds(0, 30, 100, 30);
        leftPanel.add(resetBtn);

        generationLabel = new JLabel();
        generationLabel.setName("GenerationLabel");
        leftPanel.add(generationLabel);

        aliveLabel = new JLabel();
        aliveLabel.setName("AliveLabel");
        leftPanel.add(aliveLabel);

        JLabel speedModeLbl = new JLabel("Speed mode:");
        speedModeLbl.setFont(new Font("Courier", NORMAL, 12));
        leftPanel.add(speedModeLbl);

        JSlider speedSlider = new JSlider(1, 20, speed);
        speedSlider.addChangeListener(changeEvent -> {
            speed = speedSlider.getValue();
        });
        leftPanel.add(speedSlider);

        //
        universePanel = new JPanel() {
            @Override
            public void paint(Graphics g) {
                final Color fillColor = Color.BLUE;
                final Color emptyColor = universePanel.getBackground();
                final int squareSize = getHeight() / SIZE;

                boolean[][] universe = population.getUniverse();
                for (int i = 0; i < universe.length; i++) {
                    for (int j = 0; j < universe.length; j++) {
                        if (universe[i][j]) {
                            g.setColor(fillColor);
                            g.fillRoundRect(i * squareSize + 1, j * squareSize + 1, squareSize - 1, squareSize - 1, 8, 8);
                        } else {
                            g.setColor(emptyColor);
                            g.fillRect(i * squareSize, j * squareSize, squareSize, squareSize);
                        }
                    }
                }
            }
        };
        universePanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        universePanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        universePanel.setBounds(140, 0, WIDTH - leftPanel.getWidth(), HEIGHT - 10);

        add(leftPanel);
        add(universePanel);

        setLocationRelativeTo(null);
        setLayout(null);
        setVisible(true);

        currentGame = new Game(this);
        currentGame.start();
        //
    }

    public void updateGenerationInfo() {
        generationLabel.setText("Generation #" + population.getGeneration());
        aliveLabel.setText("Alive: " + population.getAlive());

        repaint();
    }

    public void setPopulation(Population population) {
        this.population = population;
    }

    public boolean isPause() {
        return pause;
    }

    public int getSpeed() {
        return speed;
    }

    public static void main(String[] args) {
        new GameOfLife();
    }
}
