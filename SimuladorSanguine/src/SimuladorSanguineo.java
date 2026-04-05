import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

// ═══════════════════════════════════════════════════════
//  SIMULADOR HEMATOLÓGICO — CÉLULAS SANGUÍNEAS
//  Requer Java 14+
// ═══════════════════════════════════════════════════════

public class SimuladorSanguineo extends JFrame {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SimuladorSanguineo().setVisible(true));
    }

    SimuladorSanguineo() {
        setTitle("Simulador Hematológico — Células Sanguíneas");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        PainelSimulacao sim = new PainelSimulacao();
        PainelRodape rodape = new PainelRodape(sim);
        setLayout(new BorderLayout());
        add(sim, BorderLayout.CENTER);
        add(rodape, BorderLayout.SOUTH);
        pack();
        setMinimumSize(new Dimension(900, 620));
        setLocationRelativeTo(null);
    }
}

// ═══════════════════════════════════════════════════════
//  ENUMS
// ═══════════════════════════════════════════════════════

enum SubtipoEritro { NORMAL, MICROCITICO, MACROCITICO, FALCIFORME }
enum SubtipoLeuco  { NEUTROFILO, LINFOCITO, MONOCITO, EOSINOFILO, BASOFILO }

// ═══════════════════════════════════════════════════════
//  DADOS DE MODO CLÍNICO
// ═══════════════════════════════════════════════════════

class DadosModo {
    final String nome, descModo, notaLab;
    final int rbcN, rbcMi, rbcMa, rbcF;
    final int neu, lin, mon, eos, bas, plt;
    final String hb, ht, mcvStr;

    DadosModo(String nome, String desc, String lab,
              int rbcN, int rbcMi, int rbcMa, int rbcF,
              int neu, int lin, int mon, int eos, int bas, int plt,
              String hb, String ht, String mcv) {
        this.nome = nome; this.descModo = desc; this.notaLab = lab;
        this.rbcN = rbcN; this.rbcMi = rbcMi; this.rbcMa = rbcMa; this.rbcF = rbcF;
        this.neu = neu; this.lin = lin; this.mon = mon;
        this.eos = eos; this.bas = bas; this.plt = plt;
        this.hb = hb; this.ht = ht; this.mcvStr = mcv;
    }

    static final DadosModo[] TODOS = {

        new DadosModo("Sangue Normal",
            "<html><b>Sangue Normal</b><br>"
            + "Proporcao fisiologica de todos os elementos figurados.<br>"
            + "Eritrocitos biconvavos normociticos (MCV 80-100 fL), leucograma com<br>"
            + "predominio de neutrofilos (50-70%) e plaquetas em contagem adequada.<br>"
            + "Hemoglobina e hematocrito dentro dos valores de referencia da OMS.</html>",
            "Hemograma: dentro dos valores de referencia | PCR: normal | VHS: normal",
            38, 0, 0, 0,  3, 2, 1, 0, 0,  18,
            "14,5 g/dL", "44%", "88 fL | Normal"),

        new DadosModo("Anemia Ferropriva",
            "<html><b>Anemia Ferropriva</b><br>"
            + "Causa mais comum de anemia no mundo (50% dos casos globais).<br>"
            + "Deficiencia de ferro reduz sintese de hemoglobina.<br>"
            + "Eritrocitos pequenos e palidos: microciticos e hipocrômicos.<br>"
            + "RDW elevado (anisocitose). Ferritina baixa, TIBC alto, ferro serico baixo.</html>",
            "Ferritina baixa | TIBC alto | RDW >14,5% | MCV <80 fL | MCH baixo | MCHC baixo",
            0, 15, 0, 0,  3, 2, 1, 0, 0,  20,
            "8,5 g/dL (baixo)", "27% (baixo)", "<80 fL | Microcitico"),

        new DadosModo("Anemia Megaloblastica",
            "<html><b>Anemia Megaloblastica</b><br>"
            + "Deficiencia de vitamina B12 ou folato compromete sintese de DNA.<br>"
            + "Eritroblastos crescem sem dividir: eritrocitos macrociticos (MCV >100 fL).<br>"
            + "Achado patognomico: neutrofilos hipersegmentados (5 ou mais lobulos).<br>"
            + "Pancitopenia possivel: eritropoese, leucopoese e trombocitopoese comprometidas.</html>",
            "B12 baixo ou Folato baixo | MCV >100 fL | Neutrofilos hipersegmentados | LDH alto",
            0, 0, 16, 0,  3, 2, 1, 0, 0,  14,
            "9,0 g/dL (baixo)", "28% (baixo)", ">100 fL | Macrocitico"),

        new DadosModo("Anemia Falciforme",
            "<html><b>Anemia Falciforme</b><br>"
            + "Mutacao GAG para GTG no gene da beta-globina (Glu6Val, HbS).<br>"
            + "HbS polimeriza sob hipoxia: deformacao em foice e oclusao microvascular.<br>"
            + "Hemolise cronica: vida do eritrocito reduzida de 120 dias para 10-20 dias.<br>"
            + "Complicacoes: crise algica, sindrome toracica aguda, AVC, infeccoes.</html>",
            "Eletroforese Hb: HbS alto | Reticulocitos muito altos | LDH alto | Bilirrubina indireta alta",
            4, 0, 0, 18,  4, 2, 1, 0, 0,  22,
            "8,0 g/dL (baixo)", "25% (baixo)", "Variavel | Falciforme"),

        new DadosModo("Infeccao Bacteriana",
            "<html><b>Infeccao Bacteriana - Leucocitose Neutrofilica</b><br>"
            + "Resposta imune inata: medula ossea libera neutrofilos maduros e imaturos.<br>"
            + "Leucocitose acima de 11.000/uL com neutrofilia acima de 70%.<br>"
            + "Desvio a esquerda indica liberacao de bastoes (neutrofilos imaturos).<br>"
            + "Granulacoes toxicas e corpusculos de Dohle indicam infeccao grave.</html>",
            "PCR muito alto | Procalcitonina alta | VHS alto | Desvio a esquerda | Granulacoes toxicas",
            36, 0, 0, 0,  9, 2, 1, 0, 0,  20,
            "13,0 g/dL", "40%", "88 fL | Normal"),

        new DadosModo("Infeccao Viral",
            "<html><b>Infeccao Viral - Linfocitose Reativa</b><br>"
            + "Imunidade adaptativa dominante: linfocitos T CD8+ ativados contra antigenos virais.<br>"
            + "Leucopenia ou leucocitos normais. Neutropenia relativa com linfocitose absoluta.<br>"
            + "Linfocitos atipicos (celulas de Downey): T-cells ativadas, achado classico no EBV.<br>"
            + "PCR normal ou levemente elevada. Anticorpos virais especificos confirmam diagnostico.</html>",
            "Linfocitose reativa | Neutropenia relativa | PCR normal/levemente alto | IgM viral positivo",
            34, 0, 0, 0,  2, 9, 1, 0, 0,  16,
            "12,5 g/dL", "38%", "88 fL | Normal"),

        new DadosModo("Reacao Alergica",
            "<html><b>Reacao Alergica / Parasitose - Eosinofilia e Basofilia</b><br>"
            + "Eosinofilia acima de 500/uL: resposta IgE-mediada ou infeccao parasitaria.<br>"
            + "Eosinofilos liberam proteina basica maior (MBP) e peroxidase eosinofilica.<br>"
            + "Basofilos desgranulam histamina via FcepsilonRI: vasodilatacao, broncoespasmo, urticaria.<br>"
            + "IgE total muito elevada. Teste RAST/ImmunoCAP identifica alérgenos especificos.</html>",
            "IgE total muito alto | Eosinofilos >500/uL | Histamina alta | RAST positivo | Triptase alta",
            36, 0, 0, 0,  2, 2, 1, 6, 3,  18,
            "13,5 g/dL", "41%", "88 fL | Normal"),

        new DadosModo("Policitemia Vera",
            "<html><b>Policitemia Vera</b><br>"
            + "Neoplasia mieloproliferativa clonal com mutacao JAK2 V617F em mais de 95% dos casos.<br>"
            + "Producao desregulada de eritrocitos: hematocrito acima de 52% (H) ou 48% (M).<br>"
            + "Hiperviscosidade sanguinea: risco trombotico elevado (AVC, TEP, IAM, Budd-Chiari).<br>"
            + "Tratamento: flebotomia, hidroxiureia, ruxolitinibe (inibidor JAK1/2).</html>",
            "JAK2 V617F positivo | Ht >52% | EPO baixo | Esplenomegalia | Plaquetas e leucocitos levemente altos",
            72, 0, 0, 0,  4, 2, 1, 0, 0,  24,
            "19,5 g/dL (muito alto)", "62% (muito alto)", "Normal-alto"),

        new DadosModo("Trombocitopenia",
            "<html><b>Trombocitopenia</b><br>"
            + "Plaquetas abaixo de 150.000/uL. Risco hemorragico crescente com a queda.<br>"
            + "50.000 a 150.000/uL: cirurgia contraindicada. Abaixo de 20.000/uL: sangramento espontaneo.<br>"
            + "Causas: PTI (autoimune por IgG anti-GPIIb/IIIa), dengue, TTP, hipersplenismo, HIT.<br>"
            + "Volume medio plaquetario alto indica producao compensatoria na medula.</html>",
            "Plaquetas <150k/uL | VMP alto | TP e TTPA normais (PTI) | Anti-GPIIb/IIIa positivo (PTI)",
            36, 0, 0, 0,  3, 2, 1, 0, 0,  3,
            "13,0 g/dL", "40%", "88 fL | Normal"),

        new DadosModo("CIVD (CID)",
            "<html><b>CIVD - Coagulacao Intravascular Disseminada</b><br>"
            + "Ativacao sistemica descontrolada da coagulacao: consumo de fatores e plaquetas.<br>"
            + "Paradoxo: trombose difusa e hemorragia simultaneas no mesmo paciente.<br>"
            + "Esquizocitos no esfregaco = hemolise microangiopatica por fibrina intravascular.<br>"
            + "Desencadeadores: sepse grave, trauma, complicacoes obstetricas, neoplasias.</html>",
            "D-dimero muito alto | Fibrinogenio baixo | TP alto | TTPA alto | Plaquetas baixas | Esquizocitos",
            26, 0, 0, 0,  7, 2, 1, 0, 0,  2,
            "10,5 g/dL (baixo)", "32% (baixo)", "Normal")
    };
}

// ═══════════════════════════════════════════════════════
//  CÉLULA BASE (ABSTRATA)
// ═══════════════════════════════════════════════════════

abstract class Celula {
    double x, y, vx, vy, angulo, velAngular, raio;
    boolean selecionada = false;
    static final Random RNG = new Random();

    abstract void atualizar(double larg, double y1, double y2);
    abstract void desenhar(Graphics2D g2);
    abstract String getTitulo();
    abstract String getDescricao();
    abstract Color  getCorDestaque();

    void atualizarBase(double larg, double y1, double y2) {
        x += vx; y += vy; angulo += velAngular;
        if (x > larg + raio * 2) x = -raio * 2;
        if (y - raio < y1) { y = y1 + raio; vy =  Math.abs(vy) * 0.65 + 0.05; }
        if (y + raio > y2) { y = y2 - raio; vy = -Math.abs(vy) * 0.65 - 0.05; }
        vy += (RNG.nextDouble() - 0.5) * 0.1;
        vy = Math.max(-1.0, Math.min(1.0, vy));
        vy += ((y1 + y2) / 2.0 - y) * 0.0007;
    }

    boolean contem(double px, double py) {
        return Math.hypot(px - x, py - y) <= raio + 5;
    }

    static double fluxoPoiseuille(double y, double y1, double y2, double vBase) {
        double mid  = (y1 + y2) / 2.0;
        double half = (y2 - y1) / 2.0;
        double rel  = (y - mid) / half;
        return (vBase * (1.0 - rel * rel) + 0.25) * (0.75 + RNG.nextDouble() * 0.5);
    }
}

// ═══════════════════════════════════════════════════════
//  ERITRÓCITO
// ═══════════════════════════════════════════════════════

class Eritrocito extends Celula {

    final SubtipoEritro subtipo;
    private final double escX, escY;

    Eritrocito(double larg, double y1, double y2, SubtipoEritro sub) {
        this.subtipo = sub;
        raio = switch (sub) {
            case MICROCITICO ->  7 + RNG.nextDouble() * 2.5;
            case MACROCITICO -> 16 + RNG.nextDouble() * 4.0;
            case FALCIFORME  -> 10 + RNG.nextDouble() * 3.0;
            default          -> 11 + RNG.nextDouble() * 3.0;
        };
        escX = 0.9  + RNG.nextDouble() * 0.25;
        escY = (sub == SubtipoEritro.FALCIFORME) ? 0.22 + RNG.nextDouble() * 0.1
                                                 : 0.52 + RNG.nextDouble() * 0.15;
        double altVaso = y2 - y1;
        x = RNG.nextDouble() * larg;
        y = y1 + raio + RNG.nextDouble() * Math.max(1, altVaso - raio * 2);
        vx = fluxoPoiseuille(y, y1, y2, 1.4);
        vy = (RNG.nextDouble() - 0.5) * 0.35;
        angulo    = RNG.nextDouble() * Math.PI * 2;
        velAngular = (RNG.nextDouble() - 0.5) * 0.01;
    }

    @Override void atualizar(double larg, double y1, double y2) { atualizarBase(larg, y1, y2); }
    @Override Color getCorDestaque() { return new Color(180, 40, 20); }

    @Override String getTitulo() {
        return switch (subtipo) {
            case MICROCITICO -> "Eritrocito Microcitico — Anemia Ferropriva";
            case MACROCITICO -> "Eritrocito Macrocitico — Anemia Megaloblastica";
            case FALCIFORME  -> "Eritrocito Falciforme — HbS";
            default          -> "Eritrocito Normal (Hemacia)";
        };
    }

    @Override String getDescricao() {
        return switch (subtipo) {
            case MICROCITICO ->
                "<html><b>Eritrocito Microcitico — MCV abaixo de 80 fL</b><br>"
                + "Producao de hemoglobina reduzida por falta de ferro.<br>"
                + "Tamanho diminuido e palidez central aumentada (hipocromia).<br>"
                + "MCH baixo, MCHC baixo, RDW alto. Ferritina baixa, TIBC alto.<br>"
                + "Diagnostico diferencial: talassemia (MCV baixo, RBC alto, ferritina normal).</html>";
            case MACROCITICO ->
                "<html><b>Eritrocito Macrocitico — MCV acima de 100 fL</b><br>"
                + "Deficiencia de B12 ou folato: sintese de DNA comprometida.<br>"
                + "Eritroblastos crescem sem dividir: celulas gigantes (megaloblastos).<br>"
                + "Neutrofilo hipersegmentado (5 ou mais lobulos) e achado patognomico.<br>"
                + "Investigar: B12 serica, folato, fator intrinseco, anti-celula parietal.</html>";
            case FALCIFORME ->
                "<html><b>Eritrocito Falciforme — HbS</b><br>"
                + "Mutacao p.Glu6Val no gene HBB (GAG para GTG). Heranca autossomica recessiva.<br>"
                + "HbS polimeriza sob hipoxia: rigidez e deformacao em foice.<br>"
                + "Vida util: 10-20 dias versus 120 dias do eritrocito normal.<br>"
                + "Oclusao microvascular: dor aguda, infarto esplenico, necrose avascular.</html>";
            default ->
                "<html><b>Eritrocito Normal</b><br>"
                + "Disco biconvavo: 7-8 µm de diametro, ~2 µm de espessura. Area: ~140 µm².<br>"
                + "Transporta O2 via hemoglobina (270 milhoes de moleculas/celula, 4 O2 por Hb).<br>"
                + "Sem nucleo nem mitocondrias: 33% do volume e hemoglobina.<br>"
                + "Vida util: ~120 dias. Producao: 2 milhoes por segundo na medula ossea.</html>";
        };
    }

    @Override void desenhar(Graphics2D g2) {
        AffineTransform orig = g2.getTransform();
        g2.translate(x, y);
        g2.rotate(angulo);
        if (subtipo == SubtipoEritro.FALCIFORME) desenharFalciforme(g2);
        else desenharDisco(g2);
        if (selecionada) {
            g2.setColor(new Color(255, 255, 255, 200));
            g2.setStroke(new BasicStroke(1.8f));
            int rr = (int) raio + 5;
            g2.drawOval(-rr, -rr, rr * 2, rr * 2);
        }
        g2.setTransform(orig);
    }

    private void desenharDisco(Graphics2D g2) {
        g2.scale(escX, escY);
        int r = (int) raio;
        Color c1 = switch (subtipo) {
            case MICROCITICO -> new Color(230, 150, 130);
            case MACROCITICO -> new Color(255,  90,  55);
            default          -> new Color(255, 100,  70);
        };
        Color c2 = switch (subtipo) {
            case MICROCITICO -> new Color(160, 60, 40);
            case MACROCITICO -> new Color(130, 15,  0);
            default          -> new Color(100,  0,  0);
        };
        Color cC = switch (subtipo) {
            case MICROCITICO -> new Color(245, 185, 165, 190);
            case MACROCITICO -> new Color(255, 135, 105, 200);
            default          -> new Color(255, 120,  90, 200);
        };
        g2.setPaint(new GradientPaint(-r, -r, c1, r, r, c2));
        g2.fillOval(-r, -r, r * 2, r * 2);
        int ri = (int)(raio * 0.42);
        g2.setColor(cC);
        g2.fillOval(-ri, -ri, ri * 2, ri * 2);
        g2.setColor(new Color(70, 0, 0, 100));
        g2.setStroke(new BasicStroke(0.8f));
        g2.drawOval(-r, -r, r * 2, r * 2);
    }

    private void desenharFalciforme(Graphics2D g2) {
        g2.scale(escX, 1.0);
        int r = (int) raio;
        Path2D foice = new Path2D.Double();
        foice.moveTo(-r, 0);
        foice.curveTo(-r, -r * 1.6,  r, -r * 1.6,  r, 0);
        foice.curveTo( r,  r * 0.5, -r * 0.3,  r * 0.3, -r, 0);
        g2.setPaint(new GradientPaint(-r, -r, new Color(190, 25, 10), r, r / 2, new Color(75, 0, 0)));
        g2.fill(foice);
        g2.setColor(new Color(50, 0, 0, 160));
        g2.setStroke(new BasicStroke(0.8f));
        g2.draw(foice);
    }
}

// ═══════════════════════════════════════════════════════
//  LEUCÓCITO (5 subtipos)
// ═══════════════════════════════════════════════════════

class Leucocito extends Celula {

    final SubtipoLeuco subtipo;
    private final List<double[]> lobulos  = new ArrayList<>();
    private final List<int[]>    granulos = new ArrayList<>();

    Leucocito(double larg, double y1, double y2, SubtipoLeuco sub) {
        this.subtipo = sub;
        raio = switch (sub) {
            case MONOCITO   -> 18 + RNG.nextDouble() * 5;
            case LINFOCITO  ->  8 + RNG.nextDouble() * 4;
            case EOSINOFILO -> 12 + RNG.nextDouble() * 3;
            case BASOFILO   -> 10 + RNG.nextDouble() * 3;
            default         -> 13 + RNG.nextDouble() * 4;
        };
        double altVaso = y2 - y1;
        x = RNG.nextDouble() * larg;
        y = y1 + raio + RNG.nextDouble() * Math.max(1, altVaso - raio * 2);
        vx = fluxoPoiseuille(y, y1, y2, 0.7);
        vy = (RNG.nextDouble() - 0.5) * 0.3;
        angulo     = RNG.nextDouble() * Math.PI * 2;
        velAngular = (RNG.nextDouble() - 0.5) * 0.006;
        gerarNucleo();
        gerarGranulos();
    }

    private void gerarNucleo() {
        int nL = switch (subtipo) {
            case NEUTROFILO -> 3 + RNG.nextInt(2);
            case EOSINOFILO -> 2;
            case BASOFILO   -> 2;
            default         -> 1;
        };
        double lobR = switch (subtipo) {
            case LINFOCITO -> raio * 0.76;
            case MONOCITO  -> raio * 0.52;
            default        -> raio * (0.28 + RNG.nextDouble() * 0.18);
        };
        double dist = switch (subtipo) {
            case LINFOCITO -> raio * 0.05;
            case MONOCITO  -> raio * 0.22;
            default        -> raio * 0.38;
        };
        for (int i = 0; i < nL; i++) {
            double extra = (subtipo == SubtipoLeuco.NEUTROFILO) ? RNG.nextDouble() * 0.7 : 0.1;
            double a = (i / (double) nL) * Math.PI * 2 + extra;
            lobulos.add(new double[]{ Math.cos(a) * dist, Math.sin(a) * dist, lobR });
        }
    }

    private void gerarGranulos() {
        if (subtipo != SubtipoLeuco.EOSINOFILO && subtipo != SubtipoLeuco.BASOFILO) return;
        int n = (subtipo == SubtipoLeuco.EOSINOFILO) ? 10 : 8;
        for (int i = 0; i < n; i++) {
            double a = (i / (double) n) * Math.PI * 2 + RNG.nextDouble() * 0.4;
            double d = raio * (0.35 + RNG.nextDouble() * 0.3);
            granulos.add(new int[]{ (int)(Math.cos(a) * d), (int)(Math.sin(a) * d), 3 });
        }
    }

    @Override void atualizar(double larg, double y1, double y2) { atualizarBase(larg, y1, y2); }

    @Override Color getCorDestaque() {
        return switch (subtipo) {
            case EOSINOFILO -> new Color(180, 70, 15);
            case BASOFILO   -> new Color(40,  20, 130);
            default         -> new Color(70,  50, 180);
        };
    }

    @Override String getTitulo() {
        return switch (subtipo) {
            case NEUTROFILO -> "Neutrofilo (Granulocito)";
            case LINFOCITO  -> "Linfocito (Agranulocito)";
            case MONOCITO   -> "Monocito (Agranulocito)";
            case EOSINOFILO -> "Eosinofilo (Granulocito)";
            case BASOFILO   -> "Basofilo (Granulocito)";
        };
    }

    @Override String getDescricao() {
        return switch (subtipo) {
            case NEUTROFILO ->
                "<html><b>Neutrofilo</b> — 50-70% dos leucocitos circulantes<br>"
                + "Defesa imediata (inata) contra infeccoes bacterianas. Vida: 6-8 horas em circulacao.<br>"
                + "Mecanismos: fagocitose, degranulacao (elastase, MPO, lactoferrina), NETs.<br>"
                + "Burst oxidativo: NADPH oxidase gera superoxido, convertido a HOCl por MPO.<br>"
                + "Neutrofilia acima de 7.000/uL: bacterias, trauma. Abaixo de 1.500/uL: risco grave.</html>";
            case LINFOCITO  ->
                "<html><b>Linfocito</b> — 20-40% dos leucocitos<br>"
                + "Imunidade adaptativa. T-CD4+ (auxiliar), T-CD8+ (citotoxico), celulas B, NK.<br>"
                + "Nucleo grande e redondo, citoplasma escasso (relacao nucleo:citoplasma alta).<br>"
                + "Celulas B diferenciam-se em plasmocitos: produzem anticorpos (IgM, IgG, IgA, IgE).<br>"
                + "Linfocitose: EBV, CMV, LLC. Linfopenia: HIV (CD4 baixo), corticoides, sepse.</html>";
            case MONOCITO   ->
                "<html><b>Monocito</b> — 2-8% dos leucocitos<br>"
                + "Maior leucocito circulante. Circula 1-3 dias e migra para tecidos.<br>"
                + "Diferencia-se em macrofagos (M1 pro-inflamatorio/M2 anti-inflamatorio) e DCs.<br>"
                + "Nucleo em ferradura ou rim. Apresenta antigenos via MHC-II ao linfocito T-CD4+.<br>"
                + "Monocitose: infeccoes cronicas, tuberculose, doencas inflamatorias intestinais.</html>";
            case EOSINOFILO ->
                "<html><b>Eosinofilo</b> — 1-4% dos leucocitos (100-500/uL normal)<br>"
                + "Resposta a parasitas helmintos e reacoes alergicas (hipersensibilidade Tipo I e IV).<br>"
                + "Granulos: proteina basica maior (MBP), peroxidase eosinofilica, neurotoxina derivada.<br>"
                + "Eosinofilia acima de 500/uL: asma, rinite alergica, parasitoses, sindrome hipereosinofilica.<br>"
                + "Acima de 1.500/uL pode causar dano cardiaco, pulmonar e neurologico.</html>";
            case BASOFILO   ->
                "<html><b>Basofilo</b> — abaixo de 1% dos leucocitos (mais raros em circulacao)<br>"
                + "Granulos ricos em histamina, heparina e leucotrienos. Nucleo em S ou bilobulado.<br>"
                + "Receptores de alta afinidade para IgE (FcepsilonRI): desgranulacao na hipersensibilidade I.<br>"
                + "Contraparte circulante do mastocito tecidual. Papel na anafilaxia sistemica.<br>"
                + "Basofilia acima de 100/uL: leucemia mieloide cronica (LMC) e causa classica.</html>";
        };
    }

    @Override void desenhar(Graphics2D g2) {
        AffineTransform orig = g2.getTransform();
        g2.translate(x, y);
        int r = (int) raio;

        Color corCito = switch (subtipo) {
            case NEUTROFILO -> new Color(228, 215, 200, 235);
            case LINFOCITO  -> new Color(200, 215, 245, 235);
            case MONOCITO   -> new Color(215, 205, 185, 235);
            case EOSINOFILO -> new Color(245, 210, 185, 235);
            case BASOFILO   -> new Color(185, 185, 228, 235);
        };
        Color corBorda = switch (subtipo) {
            case EOSINOFILO -> new Color(180, 100,  60, 130);
            case BASOFILO   -> new Color( 80,  60, 165, 130);
            default         -> new Color(130, 140, 195, 110);
        };
        RadialGradientPaint rg = new RadialGradientPaint(
            new Point2D.Double(-r * 0.3, -r * 0.3), r,
            new float[]{0f, 1f},
            new Color[]{corCito, corBorda.brighter()});
        g2.setPaint(rg);
        g2.fillOval(-r, -r, r * 2, r * 2);
        g2.setColor(corBorda);
        g2.setStroke(new BasicStroke(0.8f));
        g2.drawOval(-r, -r, r * 2, r * 2);

        if (!granulos.isEmpty()) {
            Color cG = (subtipo == SubtipoLeuco.EOSINOFILO)
                ? new Color(200, 75, 20, 175) : new Color(35, 18, 110, 210);
            g2.setColor(cG);
            for (int[] gran : granulos)
                g2.fillOval(gran[0] - gran[2], gran[1] - gran[2], gran[2] * 2 + 1, gran[2] * 2 + 1);
        }

        g2.rotate(angulo);
        Color corNuc = switch (subtipo) {
            case EOSINOFILO -> new Color(100, 48, 145, 210);
            case BASOFILO   -> new Color( 28, 10,  98, 230);
            default         -> new Color( 62, 38, 155, 215);
        };
        g2.setColor(corNuc);
        for (double[] l : lobulos) {
            int lr = (int) l[2];
            g2.fillOval((int)(l[0] - lr), (int)(l[1] - lr), lr * 2, lr * 2);
        }

        if (selecionada) {
            g2.setTransform(orig);
            g2.translate(x, y);
            g2.setColor(new Color(255, 255, 255, 200));
            g2.setStroke(new BasicStroke(1.8f));
            g2.drawOval(-r - 5, -r - 5, (r + 5) * 2, (r + 5) * 2);
        }
        g2.setTransform(orig);
    }
}

// ═══════════════════════════════════════════════════════
//  PLAQUETA
// ═══════════════════════════════════════════════════════

class Plaqueta extends Celula {

    private final double[] pts;

    Plaqueta(double larg, double y1, double y2) {
        raio = 4 + RNG.nextDouble() * 3;
        double altVaso = y2 - y1;
        x = RNG.nextDouble() * larg;
        y = y1 + raio + RNG.nextDouble() * Math.max(1, altVaso - raio * 2);
        vx = fluxoPoiseuille(y, y1, y2, 1.0);
        vy = (RNG.nextDouble() - 0.5) * 0.4;
        angulo     = RNG.nextDouble() * Math.PI * 2;
        velAngular = (RNG.nextDouble() - 0.5) * 0.04;
        pts = new double[7];
        for (int i = 0; i < 7; i++) pts[i] = 0.55 + RNG.nextDouble() * 0.9;
    }

    @Override void atualizar(double larg, double y1, double y2) { atualizarBase(larg, y1, y2); }
    @Override Color getCorDestaque() { return new Color(145, 105, 12); }
    @Override String getTitulo()     { return "Plaqueta (Trombocito)"; }

    @Override String getDescricao() {
        return "<html><b>Plaqueta (Trombocito)</b><br>"
             + "Fragmento anucleado derivado do megacariocito (2.000-5.000 plaquetas por celula mae).<br>"
             + "Hemostasia primaria: adesao via vWF e GPIb, ativacao, agregacao via GPIIb/IIIa.<br>"
             + "Granulos alfa: fibrinogenio, vWF, P-selectina, Fator V. Granulos densos: ADP, serotonina, Ca²⁺.<br>"
             + "Vida util: 7-10 dias. Contagem normal: 150.000-400.000/uL.<br>"
             + "Funcao: tampao plaquetario temporario, base para a coagulacao secundaria (cascata).</html>";
    }

    @Override void desenhar(Graphics2D g2) {
        AffineTransform orig = g2.getTransform();
        g2.translate(x, y);
        g2.rotate(angulo);
        int n = pts.length;
        int[] px = new int[n], py = new int[n];
        for (int i = 0; i < n; i++) {
            double a = (i / (double) n) * Math.PI * 2;
            px[i] = (int)(Math.cos(a) * raio * pts[i]);
            py[i] = (int)(Math.sin(a) * raio * pts[i]);
        }
        g2.setColor(new Color(205, 170, 42));
        g2.fillPolygon(px, py, n);
        g2.setColor(new Color(140, 100, 8, 190));
        g2.setStroke(new BasicStroke(0.7f));
        g2.drawPolygon(px, py, n);
        if (selecionada) {
            int rr = (int) raio + 4;
            g2.setColor(new Color(255, 255, 255, 200));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawOval(-rr, -rr, rr * 2, rr * 2);
        }
        g2.setTransform(orig);
    }
}

// ═══════════════════════════════════════════════════════
//  PAINEL DE SIMULAÇÃO
// ═══════════════════════════════════════════════════════

class PainelSimulacao extends JPanel {

    private final List<Celula> celulas = new CopyOnWriteArrayList<>();
    boolean pausado = false;
    Celula celulaSelecionada = null;
    DadosModo modoAtual = DadosModo.TODOS[0];
    private double Y1 = 30, Y2 = 270;

    PainelSimulacao() {
        setPreferredSize(new Dimension(900, 310));
        setBackground(new Color(18, 4, 6));
        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                double mx = e.getX(), my = e.getY();
                Celula found = null;
                for (int i = celulas.size() - 1; i >= 0; i--)
                    if (celulas.get(i).contem(mx, my)) { found = celulas.get(i); break; }
                if (celulaSelecionada != null) celulaSelecionada.selecionada = false;
                celulaSelecionada = found;
                if (found != null) found.selecionada = true;
                repaint();
            }
        });
        new javax.swing.Timer(16, e -> {
            if (!pausado) {
                double larg = getWidth();
                for (Celula c : celulas) c.atualizar(larg, Y1, Y2);
            }
            repaint();
        }).start();
        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { recalcularVaso(); }
        });
    }

    void recalcularVaso() {
        int h = getHeight();
        Y1 = h * 0.09;
        Y2 = h * 0.91;
        for (Celula c : celulas) {
            if (c.y - c.raio < Y1) c.y = Y1 + c.raio + 2;
            if (c.y + c.raio > Y2) c.y = Y2 - c.raio - 2;
        }
    }

    void carregarModo(DadosModo modo) {
        modoAtual = modo;
        celulas.clear();
        celulaSelecionada = null;
        recalcularVaso();
        double larg = Math.max(getWidth(), 200);
        adicionar(larg, modo.rbcN,  SubtipoEritro.NORMAL);
        adicionar(larg, modo.rbcMi, SubtipoEritro.MICROCITICO);
        adicionar(larg, modo.rbcMa, SubtipoEritro.MACROCITICO);
        adicionar(larg, modo.rbcF,  SubtipoEritro.FALCIFORME);
        adicionar(larg, modo.neu, SubtipoLeuco.NEUTROFILO);
        adicionar(larg, modo.lin, SubtipoLeuco.LINFOCITO);
        adicionar(larg, modo.mon, SubtipoLeuco.MONOCITO);
        adicionar(larg, modo.eos, SubtipoLeuco.EOSINOFILO);
        adicionar(larg, modo.bas, SubtipoLeuco.BASOFILO);
        for (int i = 0; i < modo.plt; i++) celulas.add(new Plaqueta(larg, Y1, Y2));
    }

    private void adicionar(double larg, int n, SubtipoEritro sub) {
        for (int i = 0; i < n; i++) celulas.add(new Eritrocito(larg, Y1, Y2, sub));
    }

    private void adicionar(double larg, int n, SubtipoLeuco sub) {
        for (int i = 0; i < n; i++) celulas.add(new Leucocito(larg, Y1, Y2, sub));
    }

    void adicionarCelula(String tipo) {
        double larg = Math.max(getWidth(), 200);
        recalcularVaso();
        switch (tipo) {
            case "rbc" -> adicionar(larg, 6,  SubtipoEritro.NORMAL);
            case "neu" -> adicionar(larg, 2,  SubtipoLeuco.NEUTROFILO);
            case "lin" -> adicionar(larg, 2,  SubtipoLeuco.LINFOCITO);
            case "eos" -> adicionar(larg, 2,  SubtipoLeuco.EOSINOFILO);
            case "plt" -> { for (int i = 0; i < 12; i++) celulas.add(new Plaqueta(larg, Y1, Y2)); }
        }
    }

    long contar(Class<?> tipo) { return celulas.stream().filter(tipo::isInstance).count(); }

    long contarLeuco(SubtipoLeuco sub) {
        return celulas.stream()
            .filter(c -> c instanceof Leucocito && ((Leucocito) c).subtipo == sub).count();
    }

    int totalLeuco() {
        return (int) celulas.stream().filter(c -> c instanceof Leucocito).count();
    }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int W = getWidth(), H = getHeight();
        int iy1 = (int) Y1, iy2 = (int) Y2;

        g2.setColor(new Color(18, 4, 6));
        g2.fillRect(0, 0, W, H);

        GradientPaint gT = new GradientPaint(0, 0, new Color(40, 10, 10), 0, iy1 + 4, new Color(78, 22, 22));
        g2.setPaint(gT);
        g2.fillRect(0, 0, W, iy1 + 4);

        GradientPaint gB = new GradientPaint(0, iy2 - 4, new Color(78, 22, 22), 0, H, new Color(40, 10, 10));
        g2.setPaint(gB);
        g2.fillRect(0, iy2 - 4, W, H - iy2 + 4);

        g2.setColor(new Color(105, 35, 35));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(0, iy1, W, iy1);
        g2.drawLine(0, iy2, W, iy2);

        List<Celula> sorted = new ArrayList<>(celulas);
        sorted.sort(Comparator.comparingInt(c -> {
            if (c instanceof Plaqueta)   return 0;
            if (c instanceof Eritrocito) return 1;
            return 2;
        }));
        for (Celula c : sorted) c.desenhar(g2);

        desenharLegenda(g2);
    }

    private void desenharLegenda(Graphics2D g2) {
        int x = 12, y = (int) Y1 + 14;
        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        Object[][] leg = {
            { new Color(200, 50, 20),   "Eritrocito"  },
            { new Color(70, 50, 180),   "Leucocito"   },
            { new Color(200, 165, 40),  "Plaqueta"    }
        };
        for (Object[] item : leg) {
            g2.setColor((Color) item[0]);
            g2.fillOval(x, y - 8, 12, 9);
            g2.setColor(new Color(255, 255, 255, 180));
            g2.drawString((String) item[1], x + 16, y);
            y += 18;
        }
    }
}

// ═══════════════════════════════════════════════════════
//  PAINEL RODAPÉ (CBC + controles + info)
// ═══════════════════════════════════════════════════════

class PainelRodape extends JPanel {

    private final PainelSimulacao sim;
    private final JLabel[] vCards     = new JLabel[9];
    private final JLabel[] subLabels  = new JLabel[9];
    private final JEditorPane infoPane;
    private JButton btnPausa;

    private static final String[] NOMES_CARDS = {
        "Eritrocitos (sim)", "Leucocitos (sim)",  "Plaquetas (sim)",
        "Hemoglobina",       "Hematocrito",        "MCV",
        "Neutrofilos %",     "Linfocitos %",       "Eosinof./Basof. %"
    };
    private static final Color[] CORES = {
        new Color(185, 40, 18), new Color(65, 48, 185), new Color(140, 108, 10),
        new Color(185, 40, 18), new Color(185, 40, 18), new Color(65, 48, 185),
        new Color(65, 48, 185), new Color(65, 48, 185), new Color(140, 108, 10)
    };

    PainelRodape(PainelSimulacao sim) {
        this.sim = sim;
        setLayout(new BorderLayout(0, 6));
        setBorder(BorderFactory.createEmptyBorder(8, 10, 10, 10));

        // ── Grade CBC 3×3 ──────────────────────────────────────
        JPanel grade = new JPanel(new GridLayout(3, 3, 6, 5));
        grade.setOpaque(false);
        for (int i = 0; i < 9; i++) {
            JPanel card = new JPanel(new BorderLayout(1, 0));
            card.setBackground(UIManager.getColor("TextField.background"));
            card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(195, 195, 195), 1, true),
                BorderFactory.createEmptyBorder(5, 9, 5, 9)));
            JLabel lbl = new JLabel(NOMES_CARDS[i]);
            lbl.setFont(new Font("SansSerif", Font.PLAIN, 10));
            lbl.setForeground(UIManager.getColor("Label.disabledForeground"));
            vCards[i] = new JLabel("—");
            vCards[i].setFont(new Font("SansSerif", Font.BOLD, 17));
            vCards[i].setForeground(CORES[i]);
            subLabels[i] = new JLabel(" ");
            subLabels[i].setFont(new Font("SansSerif", Font.PLAIN, 10));
            subLabels[i].setForeground(new Color(120, 120, 120));
            card.add(lbl,          BorderLayout.NORTH);
            card.add(vCards[i],    BorderLayout.CENTER);
            card.add(subLabels[i], BorderLayout.SOUTH);
            grade.add(card);
        }

        // ── Controles ─────────────────────────────────────────
        JPanel controles = new JPanel();
        controles.setLayout(new BoxLayout(controles, BoxLayout.Y_AXIS));
        controles.setOpaque(false);

        JPanel linhaModo = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        linhaModo.setOpaque(false);
        linhaModo.add(rotulo("Modo clinico:"));
        JComboBox<String> combo = new JComboBox<>();
        for (DadosModo d : DadosModo.TODOS) combo.addItem(d.nome);
        combo.setFont(new Font("SansSerif", Font.PLAIN, 12));
        combo.setPreferredSize(new Dimension(210, 26));
        linhaModo.add(combo);

        JPanel linhaAdd = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        linhaAdd.setOpaque(false);
        linhaAdd.add(rotulo("Adicionar:"));
        String[][] bts = {
            {"+ Eritrocitos","rbc"}, {"+ Neutrofilos","neu"},
            {"+ Linfocitos","lin"}, {"+ Eosinofilos","eos"}, {"+ Plaquetas","plt"}
        };
        for (String[] b : bts) {
            JButton bt = botao(b[0]);
            final String cod = b[1];
            bt.addActionListener(e -> sim.adicionarCelula(cod));
            linhaAdd.add(bt);
        }
        linhaAdd.add(Box.createHorizontalStrut(14));
        btnPausa = botao("Pausar");
        btnPausa.addActionListener(e -> {
            sim.pausado = !sim.pausado;
            btnPausa.setText(sim.pausado ? "Retomar" : "Pausar");
        });
        linhaAdd.add(btnPausa);

        controles.add(linhaModo);
        controles.add(linhaAdd);

        // ── Painel de informação ───────────────────────────────
        infoPane = new JEditorPane("text/html", "");
        infoPane.setEditable(false);
        infoPane.setFont(new Font("SansSerif", Font.PLAIN, 12));
        infoPane.setBackground(UIManager.getColor("TextField.background"));
        infoPane.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(195, 195, 195), 1, true),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        infoPane.setPreferredSize(new Dimension(0, 75));

        JPanel sul = new JPanel(new BorderLayout(6, 6));
        sul.setOpaque(false);
        sul.add(controles, BorderLayout.NORTH);
        sul.add(infoPane,  BorderLayout.CENTER);

        add(grade, BorderLayout.NORTH);
        add(sul,   BorderLayout.CENTER);

        // ── Eventos ───────────────────────────────────────────
        combo.addActionListener(e -> {
            int idx = combo.getSelectedIndex();
            if (idx >= 0 && idx < DadosModo.TODOS.length) {
                DadosModo d = DadosModo.TODOS[idx];
                sim.carregarModo(d);
                mostrarModo(d);
            }
        });

        new javax.swing.Timer(350, e -> {
            atualizarCBC();
            Celula sel = sim.celulaSelecionada;
            if (sel != null) mostrarCelula(sel);
        }).start();

        javax.swing.Timer init = new javax.swing.Timer(200, e -> {
            sim.carregarModo(DadosModo.TODOS[0]);
            mostrarModo(DadosModo.TODOS[0]);
        });
        init.setRepeats(false);
        init.start();
    }

    private void atualizarCBC() {
        DadosModo m   = sim.modoAtual;
        long rbc      = sim.contar(Eritrocito.class);
        long wbc      = sim.contar(Leucocito.class);
        long plt      = sim.contar(Plaqueta.class);
        int  totalW   = Math.max(1, sim.totalLeuco());
        long neu      = sim.contarLeuco(SubtipoLeuco.NEUTROFILO);
        long lin      = sim.contarLeuco(SubtipoLeuco.LINFOCITO);
        long eos      = sim.contarLeuco(SubtipoLeuco.EOSINOFILO);
        long bas      = sim.contarLeuco(SubtipoLeuco.BASOFILO);

        vCards[0].setText(String.valueOf(rbc));
        subLabels[0].setText("celulas na simulacao");
        vCards[1].setText(String.valueOf(wbc));
        subLabels[1].setText("celulas na simulacao");
        vCards[2].setText(String.valueOf(plt));
        subLabels[2].setText("celulas na simulacao");

        vCards[3].setText(m.hb);
        subLabels[3].setText("Ref: 12,0-17,5 g/dL");
        vCards[4].setText(m.ht);
        subLabels[4].setText("Ref: 36-53%");

        String[] mcvParts = m.mcvStr.split("\\|");
        vCards[5].setText(mcvParts[0].trim());
        subLabels[5].setText(mcvParts.length > 1 ? mcvParts[1].trim() : "MCV");

        String neuP = String.format("%d%%", Math.round(100.0 * neu / totalW));
        String linP = String.format("%d%%", Math.round(100.0 * lin / totalW));
        String eoP  = String.format("%d%%", Math.round(100.0 * (eos + bas) / totalW));
        vCards[6].setText(neuP);
        subLabels[6].setText("Ref: 50-70%");
        vCards[7].setText(linP);
        subLabels[7].setText("Ref: 20-40%");
        vCards[8].setText(eoP);
        subLabels[8].setText("Ref: <5% (eos+bas)");

        // Alertas de cor
        boolean hbBaixo = m.hb.contains("baixo");
        boolean hbAlto  = m.hb.contains("alto");
        vCards[3].setForeground(hbBaixo ? new Color(180,30,30) : hbAlto ? new Color(180,100,0) : CORES[3]);
        boolean htBaixo = m.ht.contains("baixo");
        boolean htAlto  = m.ht.contains("alto");
        vCards[4].setForeground(htBaixo ? new Color(180,30,30) : htAlto ? new Color(180,100,0) : CORES[4]);
        vCards[2].setForeground(plt <= 3 ? new Color(180, 30, 30) : CORES[2]);
    }

    private void mostrarModo(DadosModo d) {
        infoPane.setText(
            "<html><body style='font-family:SansSerif;font-size:11pt'>"
            + d.descModo
            + "<br><span style='color:#666;font-size:10pt'>"
            + "<b>Achados laboratoriais:</b> " + d.notaLab
            + "</span></body></html>");
    }

    private void mostrarCelula(Celula c) {
        String hex = String.format("#%02x%02x%02x",
            c.getCorDestaque().getRed(), c.getCorDestaque().getGreen(), c.getCorDestaque().getBlue());
        infoPane.setText(
            "<html><body style='font-family:SansSerif;font-size:11pt'>"
            + "<b style='color:" + hex + "'>" + c.getTitulo() + "</b><br>"
            + c.getDescricao()
            + "</body></html>");
    }

    private JButton botao(String txt) {
        JButton b = new JButton(txt);
        b.setFont(new Font("SansSerif", Font.PLAIN, 12));
        b.setMargin(new Insets(3, 8, 3, 8));
        return b;
    }

    private JLabel rotulo(String txt) {
        JLabel l = new JLabel(txt);
        l.setFont(new Font("SansSerif", Font.PLAIN, 12));
        return l;
    }
}