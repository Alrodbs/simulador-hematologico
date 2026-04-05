import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SimuladorSanguineo extends JFrame {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SimuladorSanguineo janela = new SimuladorSanguineo();
            janela.setVisible(true);
        });
    }

    public SimuladorSanguineo() {
        setTitle("Simulador de Células Sanguíneas");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        PainelSimulacao painel = new PainelSimulacao();
        PainelControle controle = new PainelControle(painel);

        add(painel, BorderLayout.CENTER);
        add(controle, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        setResizable(true);
    }
}

// ─────────────────────────────────────────────
//  CÉLULA BASE
// ─────────────────────────────────────────────
abstract class Celula {

    double x, y, vx, vy, angulo, velAngular;
    double raio;
    boolean selecionada = false;

    static final Random RNG = new Random();

    Celula() { /* inicialização feita pelas subclasses */ }

    abstract double velocidadeBase();
    abstract double fatorRotacao();
    abstract void desenhar(Graphics2D g2);
    abstract String getNome();
    abstract String getDescricao();
    abstract Color getCorLabel();

    void atualizar(double largura, double y1, double y2) {
        x += vx;
        y += vy;
        angulo += velAngular;

        // Reaparecer do lado esquerdo
        if (x > largura + raio * 2) x = -raio * 2;

        // Quicar nas paredes
        if (y - raio < y1) { y = y1 + raio; vy = Math.abs(vy) * 0.6 + 0.05; }
        if (y + raio > y2) { y = y2 - raio; vy = -Math.abs(vy) * 0.6 - 0.05; }

        // Deriva aleatória
        vy += (RNG.nextDouble() - 0.5) * 0.1;
        vy = Math.max(-1.0, Math.min(1.0, vy));

        // Leve atração ao centro
        double centro = (y1 + y2) / 2.0;
        vy += (centro - y) * 0.0007;
    }

    boolean contem(double px, double py) {
        return Math.hypot(px - x, py - y) <= raio + 5;
    }
}

// ─────────────────────────────────────────────
//  ERITRÓCITO (Hemácia)
// ─────────────────────────────────────────────
class Eritrocito extends Celula {

    double escalaX, escalaY;

    Eritrocito(double larg, double y1, double y2) {
        raio = 11 + RNG.nextDouble() * 3;
        escalaX = 0.9 + RNG.nextDouble() * 0.25;
        escalaY = 0.52 + RNG.nextDouble() * 0.15;
        // Chama o construtor pai depois de definir raio
        double metade = (y1 + y2) / 2.0;
        double altVaso = y2 - y1;
        x = RNG.nextDouble() * larg;
        y = y1 + raio + RNG.nextDouble() * (altVaso - raio * 2);
        double pos = (y - metade) / (altVaso / 2.0);
        double perfil = 1.0 - pos * pos;
        vx = (velocidadeBase() * perfil + 0.35) * (0.8 + RNG.nextDouble() * 0.4);
        vy = (RNG.nextDouble() - 0.5) * 0.35;
        angulo = RNG.nextDouble() * Math.PI * 2;
        velAngular = (RNG.nextDouble() - 0.5) * 0.008;
    }

    @Override double velocidadeBase() { return 1.4; }
    @Override double fatorRotacao()   { return 1.0; }
    @Override String getNome()        { return "Eritrócito (Hemácia)"; }
    @Override Color  getCorLabel()    { return new Color(180, 40, 20); }

    @Override
    String getDescricao() {
        return "<html><b>Eritrócito (Hemácia / RBC)</b><br>"
             + "Principal célula do sangue — transporta oxigênio dos<br>"
             + "pulmões para os tecidos via hemoglobina. Formato<br>"
             + "bicôncavo, sem núcleo. Vida útil: ~120 dias.<br>"
             + "Contagem normal: 4,5–5,5 milhões/µL.</html>";
    }

    @Override
    void desenhar(Graphics2D g2) {
        AffineTransform orig = g2.getTransform();
        g2.translate(x, y);
        g2.rotate(angulo);
        g2.scale(escalaX, escalaY);

        // Corpo bicôncavo
        int r = (int) raio;
        GradientPaint grad = new GradientPaint(
                -r, -r, new Color(255, 100, 70),
                 r,  r, new Color(100, 0, 0));
        g2.setPaint(grad);
        g2.fillOval(-r, -r, r * 2, r * 2);

        // Centro mais claro (concavidade)
        int ri = (int)(raio * 0.42);
        g2.setColor(new Color(255, 120, 90, 200));
        g2.fillOval(-ri, -ri, ri * 2, ri * 2);

        // Borda
        g2.setColor(new Color(80, 0, 0, 120));
        g2.setStroke(new BasicStroke(1f));
        g2.drawOval(-r, -r, r * 2, r * 2);

        if (selecionada) {
            g2.setColor(new Color(255, 255, 255, 180));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawOval(-r - 4, -r - 4, (r + 4) * 2, (r + 4) * 2);
        }
        g2.setTransform(orig);
    }
}

// ─────────────────────────────────────────────
//  LEUCÓCITO (Glóbulo Branco)
// ─────────────────────────────────────────────
class Leucocito extends Celula {

    private final List<double[]> lobulos = new ArrayList<>();

    Leucocito(double larg, double y1, double y2) {
        raio = 17 + RNG.nextDouble() * 6;
        double metade = (y1 + y2) / 2.0;
        double altVaso = y2 - y1;
        x = RNG.nextDouble() * larg;
        y = y1 + raio + RNG.nextDouble() * (altVaso - raio * 2);
        double pos = (y - metade) / (altVaso / 2.0);
        double perfil = 1.0 - pos * pos;
        vx = (velocidadeBase() * perfil + 0.2) * (0.7 + RNG.nextDouble() * 0.6);
        vy = (RNG.nextDouble() - 0.5) * 0.3;
        angulo = RNG.nextDouble() * Math.PI * 2;
        velAngular = (RNG.nextDouble() - 0.5) * 0.005;

        // Gerar lóbulos do núcleo
        int n = 2 + RNG.nextInt(3);
        for (int i = 0; i < n; i++) {
            double a = (i / (double) n) * Math.PI * 2 + RNG.nextDouble() * 0.8;
            double lr = raio * (0.28 + RNG.nextDouble() * 0.18);
            double dx = Math.cos(a) * raio * 0.38;
            double dy = Math.sin(a) * raio * 0.38;
            lobulos.add(new double[]{dx, dy, lr});
        }
    }

    @Override double velocidadeBase() { return 0.7; }
    @Override double fatorRotacao()   { return 1.0; }
    @Override String getNome()        { return "Leucócito (Glóbulo Branco)"; }
    @Override Color  getCorLabel()    { return new Color(70, 50, 180); }

    @Override
    String getDescricao() {
        return "<html><b>Leucócito (Glóbulo Branco / WBC)</b><br>"
             + "Célula do sistema imune. Combate infecções,<br>"
             + "bactérias e vírus. Possui núcleo multilobulado<br>"
             + "(neutrófilo) ou grande e redondo (linfócito).<br>"
             + "Contagem normal: 4.000–11.000/µL.</html>";
    }

    @Override
    void desenhar(Graphics2D g2) {
        AffineTransform orig = g2.getTransform();
        g2.translate(x, y);

        int r = (int) raio;

        // Corpo translúcido
        RadialGradientPaint rg = new RadialGradientPaint(
                new Point2D.Double(-r * 0.3, -r * 0.3), r,
                new float[]{0f, 1f},
                new Color[]{new Color(220, 225, 255, 245), new Color(170, 180, 235, 210)});
        g2.setPaint(rg);
        g2.fillOval(-r, -r, r * 2, r * 2);
        g2.setColor(new Color(100, 110, 200, 100));
        g2.setStroke(new BasicStroke(1f));
        g2.drawOval(-r, -r, r * 2, r * 2);

        // Núcleo multilobulado
        g2.rotate(angulo);
        g2.setColor(new Color(65, 40, 150, 210));
        for (double[] l : lobulos) {
            int lr = (int) l[2];
            g2.fillOval((int)(l[0] - lr), (int)(l[1] - lr), lr * 2, lr * 2);
        }

        if (selecionada) {
            g2.setTransform(orig);
            g2.translate(x, y);
            g2.setColor(new Color(255, 255, 255, 180));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawOval(-r - 4, -r - 4, (r + 4) * 2, (r + 4) * 2);
        }
        g2.setTransform(orig);
    }
}

// ─────────────────────────────────────────────
//  PLAQUETA (Trombócito)
// ─────────────────────────────────────────────
class Plaqueta extends Celula {

    private final double[] pts;

    Plaqueta(double larg, double y1, double y2) {
        raio = 4 + RNG.nextDouble() * 3;
        double metade = (y1 + y2) / 2.0;
        double altVaso = y2 - y1;
        x = RNG.nextDouble() * larg;
        y = y1 + raio + RNG.nextDouble() * (altVaso - raio * 2);
        double pos = (y - metade) / (altVaso / 2.0);
        double perfil = 1.0 - pos * pos;
        vx = (velocidadeBase() * perfil + 0.3) * (0.8 + RNG.nextDouble() * 0.4);
        vy = (RNG.nextDouble() - 0.5) * 0.4;
        angulo = RNG.nextDouble() * Math.PI * 2;
        velAngular = (RNG.nextDouble() - 0.5) * 0.04;
        pts = new double[7];
        for (int i = 0; i < 7; i++) pts[i] = 0.55 + RNG.nextDouble() * 0.9;
    }

    @Override double velocidadeBase() { return 1.0; }
    @Override double fatorRotacao()   { return 3.0; }
    @Override String getNome()        { return "Plaqueta (Trombócito)"; }
    @Override Color  getCorLabel()    { return new Color(140, 100, 10); }

    @Override
    String getDescricao() {
        return "<html><b>Plaqueta (Trombócito)</b><br>"
             + "Fragmento celular derivado dos megacariócitos.<br>"
             + "Fundamental na coagulação sanguínea — acorre ao<br>"
             + "local de lesão vascular e forma tampão plaquetário.<br>"
             + "Contagem normal: 150.000–400.000/µL.</html>";
    }

    @Override
    void desenhar(Graphics2D g2) {
        AffineTransform orig = g2.getTransform();
        g2.translate(x, y);
        g2.rotate(angulo);

        int n = pts.length;
        int[] px = new int[n], py = new int[n];
        for (int i = 0; i < n; i++) {
            double a = (i / (double) n) * Math.PI * 2;
            double r = raio * pts[i];
            px[i] = (int)(Math.cos(a) * r);
            py[i] = (int)(Math.sin(a) * r);
        }
        g2.setColor(new Color(200, 165, 40));
        g2.fillPolygon(px, py, n);
        g2.setColor(new Color(140, 100, 10, 180));
        g2.setStroke(new BasicStroke(0.8f));
        g2.drawPolygon(px, py, n);

        if (selecionada) {
            g2.setTransform(orig);
            g2.translate(x, y);
            g2.setColor(new Color(255, 255, 255, 180));
            g2.setStroke(new BasicStroke(1.5f));
            int r = (int) raio;
            g2.drawOval(-r - 4, -r - 4, (r + 4) * 2, (r + 4) * 2);
        }
        g2.setTransform(orig);
    }
}

// ─────────────────────────────────────────────
//  PAINEL DE SIMULAÇÃO (Canvas principal)
// ─────────────────────────────────────────────
class PainelSimulacao extends JPanel {

    private final List<Celula> celulas = new CopyOnWriteArrayList<>();
    private final Random rng = new Random();
    private boolean pausado = false;
    private Celula celulaSelecionada = null;
    private javax.swing.Timer timer;

    // Dimensões do vaso
    private double Y1, Y2;

    PainelSimulacao() {
        setPreferredSize(new Dimension(900, 320));
        setBackground(new Color(19, 4, 6));

        // Listener de clique
        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                double mx = e.getX(), my = e.getY();
                Celula encontrada = null;
                for (Celula c : celulas) {
                    if (c.contem(mx, my)) { encontrada = c; break; }
                }
                if (celulaSelecionada != null) celulaSelecionada.selecionada = false;
                celulaSelecionada = encontrada;
                if (encontrada != null) encontrada.selecionada = true;
                repaint();
            }
        });

        // Timer de animação ~60 FPS
        timer = new javax.swing.Timer(16, e -> {
            if (!pausado) {
                double larg = getWidth();
                for (Celula c : celulas) c.atualizar(larg, Y1, Y2);
            }
            repaint();
        });
        timer.start();

        // Aguarda o layout estar pronto
        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                recalcularVaso();
            }
        });
    }

    void recalcularVaso() {
        int h = getHeight();
        Y1 = h * 0.10;
        Y2 = h * 0.90;
        // Reposicionar células que saíram dos limites
        for (Celula c : celulas) {
            if (c.y - c.raio < Y1) c.y = Y1 + c.raio + 2;
            if (c.y + c.raio > Y2) c.y = Y2 - c.raio - 2;
        }
    }

    void carregarModo(String modo) {
        celulas.clear();
        celulaSelecionada = null;
        double larg = Math.max(getWidth(), 100);
        recalcularVaso();
        switch (modo) {
            case "normal":
                adicionarCelulas("rbc", 38, larg);
                adicionarCelulas("wbc",  4, larg);
                adicionarCelulas("plt", 18, larg);
                break;
            case "anemia":
                adicionarCelulas("rbc", 12, larg);
                adicionarCelulas("wbc",  4, larg);
                adicionarCelulas("plt", 18, larg);
                break;
            case "infeccao":
                adicionarCelulas("rbc", 38, larg);
                adicionarCelulas("wbc", 18, larg);
                adicionarCelulas("plt", 18, larg);
                break;
        }
    }

    void adicionarCelulas(String tipo, int n, double larg) {
        if (larg <= 0) larg = Math.max(getWidth(), 100);
        recalcularVaso();
        for (int i = 0; i < n; i++) {
            Celula c = switch (tipo) {
                case "rbc" -> new Eritrocito(larg, Y1, Y2);
                case "wbc" -> new Leucocito(larg, Y1, Y2);
                default    -> new Plaqueta(larg, Y1, Y2);
            };
            celulas.add(c);
        }
    }

    void togglePausa() { pausado = !pausado; }
    boolean isPausado() { return pausado; }
    Celula getCelulaSelecionada() { return celulaSelecionada; }

    long contagem(Class<?> tipo) {
        return celulas.stream().filter(tipo::isInstance).count();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int W = getWidth(), H = getHeight();
        int iy1 = (int) Y1, iy2 = (int) Y2;

        // Fundo (plasma sanguíneo escuro)
        g2.setColor(new Color(19, 4, 6));
        g2.fillRect(0, 0, W, H);

        // Paredes do vaso — superior
        GradientPaint gTop = new GradientPaint(0, 0, new Color(42, 10, 10), 0, iy1 + 4, new Color(75, 22, 22));
        g2.setPaint(gTop);
        g2.fillRect(0, 0, W, iy1 + 4);

        // Paredes do vaso — inferior
        GradientPaint gBot = new GradientPaint(0, iy2 - 4, new Color(75, 22, 22), 0, H, new Color(42, 10, 10));
        g2.setPaint(gBot);
        g2.fillRect(0, iy2 - 4, W, H - iy2 + 4);

        // Linhas de borda do vaso
        g2.setColor(new Color(100, 32, 32));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(0, iy1, W, iy1);
        g2.drawLine(0, iy2, W, iy2);

        // Células — ordem: plaquetas → eritrócitos → leucócitos
        List<Celula> ordem = new ArrayList<>(celulas);
        ordem.sort(Comparator.comparingInt(c -> {
            if (c instanceof Plaqueta)    return 0;
            if (c instanceof Eritrocito)  return 1;
            return 2;
        }));
        for (Celula c : ordem) c.desenhar(g2);

        // Legenda
        desenharLegenda(g2, W, H);
    }

    private void desenharLegenda(Graphics2D g2, int W, int H) {
        int xL = 12, yL = (int)(Y1 + 10);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        FontMetrics fm = g2.getFontMetrics();

        // RBC
        g2.setColor(new Color(200, 50, 20));
        g2.fillOval(xL, yL, 14, 9);
        g2.setColor(new Color(255, 255, 255, 180));
        g2.drawString("Eritrócito", xL + 18, yL + 9);

        // WBC
        yL += 20;
        g2.setColor(new Color(190, 195, 240, 200));
        g2.fillOval(xL, yL - 1, 14, 14);
        g2.setColor(new Color(255, 255, 255, 180));
        g2.drawString("Leucócito", xL + 18, yL + 9);

        // PLT
        yL += 20;
        g2.setColor(new Color(200, 165, 40));
        g2.fillOval(xL + 3, yL + 3, 8, 8);
        g2.setColor(new Color(255, 255, 255, 180));
        g2.drawString("Plaqueta", xL + 18, yL + 9);
    }
}

// ─────────────────────────────────────────────
//  PAINEL DE CONTROLE (botões + info)
// ─────────────────────────────────────────────
class PainelControle extends JPanel {

    private final PainelSimulacao sim;
    private JLabel lblInfo = new JLabel();
    private final JLabel[] contadores = new JLabel[3];
    private JButton btnPausa;
    private javax.swing.Timer atualizador;

    PainelControle(PainelSimulacao sim) {
        this.sim = sim;
        setLayout(new BorderLayout(0, 4));
        setBackground(UIManager.getColor("Panel.background"));
        setBorder(BorderFactory.createEmptyBorder(8, 10, 10, 10));

        // ── Linha 1: contadores ──────────────────────────────────
        JPanel linhaContadores = new JPanel(new GridLayout(1, 3, 8, 0));
        linhaContadores.setOpaque(false);
        String[] nomes = {"Eritrócitos", "Leucócitos", "Plaquetas"};
        Color[] cores   = {new Color(180,40,20), new Color(70,50,180), new Color(140,100,10)};
        for (int i = 0; i < 3; i++) {
            JPanel card = new JPanel(new BorderLayout(0, 2));
            card.setBackground(UIManager.getColor("TextField.background"));
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(180, 180, 180), 1),
                    BorderFactory.createEmptyBorder(6, 10, 6, 10)));
            JLabel lbl = new JLabel(nomes[i], SwingConstants.CENTER);
            lbl.setFont(new Font("SansSerif", Font.PLAIN, 11));
            JLabel cnt = new JLabel("0", SwingConstants.CENTER);
            cnt.setFont(new Font("SansSerif", Font.BOLD, 22));
            cnt.setForeground(cores[i]);
            contadores[i] = cnt;
            card.add(lbl, BorderLayout.NORTH);
            card.add(cnt, BorderLayout.CENTER);
            linhaContadores.add(card);
        }

        // ── Linha 2: modos ───────────────────────────────────────
        JPanel linhaM = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        linhaM.setOpaque(false);
        linhaM.add(new JLabel("Modo:"));
        String[] modos = {"Sangue Normal", "Anemia", "Infecção"};
        String[] chaves = {"normal", "anemia", "infeccao"};
        for (int i = 0; i < modos.length; i++) {
            final String ch = chaves[i];
            JButton b = new JButton(modos[i]);
            b.setFont(new Font("SansSerif", Font.PLAIN, 12));
            b.addActionListener(e -> {
                sim.carregarModo(ch);
                atualizarInfo();
                if (ch.equals("anemia")) {
                    lblInfo.setText("<html><b>Anemia:</b> Eritrócitos reduzidos → menos oxigênio nos tecidos. "
                            + "Causa fadiga, palidez e falta de ar. Pode ser causada por deficiência de ferro ou vitamina B12.</html>");
                } else if (ch.equals("infeccao")) {
                    lblInfo.setText("<html><b>Leucocitose (Infecção):</b> Aumento de leucócitos para combater agentes invasores. "
                            + "Contagem acima de 11.000/µL. Sinal de ativação do sistema imune.</html>");
                } else {
                    lblInfo.setText("Clique em uma célula para ver informações detalhadas.");
                }
            });
            linhaM.add(b);
        }

        // ── Linha 3: adicionar + pausar ──────────────────────────
        JPanel linhaA = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        linhaA.setOpaque(false);
        linhaA.add(new JLabel("Adicionar:"));
        String[] tiposBtn = {"+ Eritrócitos", "+ Leucócitos", "+ Plaquetas"};
        String[] tiposCod = {"rbc", "wbc", "plt"};
        int[] qtds = {8, 2, 12};
        for (int i = 0; i < tiposBtn.length; i++) {
            final String cod = tiposCod[i];
            final int q = qtds[i];
            JButton b = new JButton(tiposBtn[i]);
            b.setFont(new Font("SansSerif", Font.PLAIN, 12));
            b.addActionListener(e -> sim.adicionarCelulas(cod, q, sim.getWidth()));
            linhaA.add(b);
        }
        // Botão Pausar
        btnPausa = new JButton("Pausar");
        btnPausa.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnPausa.addActionListener(e -> {
            sim.togglePausa();
            btnPausa.setText(sim.isPausado() ? "Retomar" : "Pausar");
        });
        linhaA.add(Box.createHorizontalStrut(16));
        linhaA.add(btnPausa);

        // ── Painel de informação ─────────────────────────────────
        lblInfo = new JLabel("Clique em uma célula para ver informações detalhadas.");
        lblInfo.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblInfo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 180, 180), 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        lblInfo.setBackground(UIManager.getColor("TextField.background"));
        lblInfo.setOpaque(true);

        // ── Montar layout ────────────────────────────────────────
        JPanel centro = new JPanel();
        centro.setLayout(new BoxLayout(centro, BoxLayout.Y_AXIS));
        centro.setOpaque(false);
        centro.add(linhaContadores);
        centro.add(Box.createVerticalStrut(6));
        centro.add(linhaM);
        centro.add(Box.createVerticalStrut(4));
        centro.add(linhaA);
        centro.add(Box.createVerticalStrut(6));
        centro.add(lblInfo);
        add(centro, BorderLayout.CENTER);

        // Timer para atualizar contadores e info
        atualizador = new javax.swing.Timer(300, e -> atualizarInfo());
        atualizador.start();

        // Inicia com sangue normal (leve atraso para o layout estar pronto)
        javax.swing.Timer init = new javax.swing.Timer(200, e -> sim.carregarModo("normal"));
        init.setRepeats(false);
        init.start();
    }

    private void atualizarInfo() {
        contadores[0].setText(String.valueOf(sim.contagem(Eritrocito.class)));
        contadores[1].setText(String.valueOf(sim.contagem(Leucocito.class)));
        contadores[2].setText(String.valueOf(sim.contagem(Plaqueta.class)));

        Celula sel = sim.getCelulaSelecionada();
        if (sel != null) {
            lblInfo.setText(sel.getDescricao());
            lblInfo.setForeground(sel.getCorLabel());
        }
    }
}