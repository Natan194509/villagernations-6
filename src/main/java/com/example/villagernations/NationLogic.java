package com.example.villagernations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.example.villagernations.net.ActionPayload;
import com.example.villagernations.net.ScreenPayload;
import com.example.villagernations.net.SyncPayload;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Simulação da vila. Habitantes e edifícios são dados; o mundo só recebe as estruturas físicas.
 * A simulação roda em ciclos de 10 segundos e não consulta nenhuma entidade.
 */
public final class NationLogic {
    /** Um ciclo de simulação = 200 ticks = 10 segundos. */
    public static final int INTERVALO = 200;
    public static final int CICLOS_ECONOMIA = 6;
    public static final int CUSTO_RECRUTA = 5;
    public static final int IMPOSTO_MAX = 3;
    public static final int SALARIO_MAX = 2;
    public static final int BASE_ARMAZEM = 200;
    /** Quantos villagers decorativos aparecem no mundo (os dados não dependem deles). */
    public static final int MAX_VISIVEIS = 8;
    public static final int POR_PAGINA = 6;
    public static final int HISTORICO_MAX = 8;
    public static final int MAX_FILA = 5;

    private static final String[] NOMES = {
            "Aldo", "Bruna", "Caio", "Dora", "Enzo", "Fina", "Gael", "Hilda", "Ivo", "Joana",
            "Kaua", "Lia", "Murilo", "Nina", "Otto", "Paula", "Raul", "Sara", "Tome", "Vera"
    };

    private static final Map<UUID, BlockPos> PENDENTE = new HashMap<>();
    private static final Map<UUID, String> AVISO = new HashMap<>();
    private static final Map<UUID, Integer> PAGINA = new HashMap<>();

    private NationLogic() {}

    // ---------------------------------------------------------------- mensagens

    /** Manda no chat e guarda como aviso para aparecer também no painel. */
    public static void msg(Player player, String texto) {
        player.sendSystemMessage(Component.literal(texto));
        AVISO.put(player.getUUID(), texto);
    }

    private static void registrar(NationData d, String texto) {
        d.historico.add(texto);
        while (d.historico.size() > HISTORICO_MAX) d.historico.remove(0);
    }

    private static void noticia(MinecraftServer server, NationData d, String texto) {
        registrar(d, texto);
        server.getPlayerList().broadcastSystemMessage(Component.literal(texto), false);
    }

    // ------------------------------------------------------------------ derivados

    /** Recalcula moradia, armazém, vagas, renda, bônus e médias a partir dos dados reais. */
    public static void calcularDerivados(NationData d) {
        Arrays.fill(d.vagas, 0);
        Arrays.fill(d.contagem, 0);
        Arrays.fill(d.edConcluidos, 0);
        Arrays.fill(d.edObra, 0);
        d.moradia = 0;
        d.armazem = BASE_ARMAZEM;
        d.concluidos = 0;
        d.renda = 0;
        int bonus = 0;

        for (Edificio e : d.edificios) {
            int t = e.tipo.ordinal();
            if (e.concluido) {
                d.edConcluidos[t]++;
                d.concluidos++;
                d.moradia += e.tipo.moradia();
                d.armazem += e.tipo.armazenamento();
                d.renda += e.tipo.renda();
                bonus += e.tipo.bonusFeliz();
                if (e.tipo.profissao() != null) {
                    d.vagas[e.tipo.profissao().ordinal()] += e.tipo.vagas();
                }
            } else {
                d.edObra[t]++;
            }
        }
        d.bonusFeliz = Math.min(25, bonus);

        int somaF = 0, somaS = 0, somaH = 0;
        for (Habitante h : d.habitantes) {
            d.contagem[h.profissao.ordinal()]++;
            somaF += h.fome;
            somaS += h.saude;
            somaH += h.felicidade;
        }
        int n = d.habitantes.size();
        d.mediaFome = n == 0 ? 100 : somaF / n;
        d.mediaSaude = n == 0 ? 100 : somaS / n;
        d.mediaFeliz = n == 0 ? 70 : somaH / n;
    }

    // ------------------------------------------------------------------- tick

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        int tick = server.getTickCount();
        if (tick % 20 != 0) return;

        NationData d = NationData.get(server);
        if (d.center == null) return;

        if (tick % INTERVALO == 0) {
            processarCiclo(server, d);
        }
        // Sincroniza a cada 2 segundos (as ações do painel sincronizam na hora).
        if (tick % 40 == 0 && server.getPlayerCount() > 0) {
            calcularDerivados(d);
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                enviarSync(p, d);
            }
        }
    }

    /** Ao entrar no mundo, quem tem uma vila volta direto para o Modo Prefeito. */
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        NationData d = NationData.get(sp.server);
        if (d.center != null && !noModoPrefeito(sp)) {
            entrarModoPrefeito(sp, d, false);
        }
    }

    /** Sem reprodução dos villagers vanilla: a população é controlada pela vila. */
    public static void onBaby(BabyEntitySpawnEvent event) {
        if (event.getParentA() instanceof Villager v && v.level() instanceof ServerLevel sl
                && NationData.get(sl.getServer()).center != null) {
            event.setCanceled(true);
        }
    }

    private static void processarCiclo(MinecraftServer server, NationData d) {
        calcularDerivados(d);
        simularHabitantes(server, d);
        d.ciclo++;
        if (d.ciclo % CICLOS_ECONOMIA == 0) {
            calcularDerivados(d);
            fecharEconomia(d);
        }
        completarPendentes(server.overworld(), d);
        d.setDirty();
    }

    private static int eficiencia(Habitante h) {
        int e = 50 + h.energia / 4 + h.saude / 4 + 5 * h.nivel();
        return e * Rules.multiplicador(h.felicidade) / 100;
    }

    private static void gastarEnergia(Habitante h) {
        h.energia = Math.max(0, h.energia - 4);
        h.xp++;
    }

    private static void simularHabitantes(MinecraftServer server, NationData d) {
        int pop = d.habitantes.size();
        if (pop == 0) return;

        // Alimentação: a comida disponível alimenta uma parte dos habitantes (rotativa)
        int necessidade = (pop * Rules.consumoPercent(d.dificuldade) + 99) / 100;
        int consumido = Math.min(d.food, necessidade);
        d.food -= consumido;
        int alimentados = necessidade == 0 ? pop : Math.min(pop, consumido * pop / necessidade);
        int offset = d.ciclo % pop;

        int[] vagasRest = d.vagas.clone();
        int moradiaSat = pop <= d.moradia ? 100 : d.moradia * 100 / pop;
        int renda = Math.max(0, Math.min(100, 60 + 20 * d.salario - 20 * d.tax));

        boolean temObra = false;
        for (Edificio e : d.edificios) {
            if (!e.concluido) {
                temObra = true;
                break;
            }
        }

        int comida100 = 0, madeira100 = 0, pedra100 = 0, obra100 = 0;
        List<Habitante> mortos = new ArrayList<>();

        for (int i = 0; i < pop; i++) {
            Habitante h = d.habitantes.get(i);

            boolean alimentado = ((i + offset) % pop) < alimentados;
            if (alimentado) h.fome = Math.min(100, h.fome + 10);
            else h.fome = Math.max(0, h.fome - 15);
            if (h.fome == 0) h.saude = Math.max(0, h.saude - 10);
            else if (h.fome >= 60) h.saude = Math.min(100, h.saude + 2);
            if (h.saude <= 0) {
                mortos.add(h);
                continue;
            }

            boolean descansa = h.ordem == Ordem.DESCANSAR || h.energia < 20;
            if (descansa) {
                h.tarefa = 3;
                h.energia = Math.min(100, h.energia + (moradiaSat >= 100 ? 20 : 10));
            } else if (h.ordem == Ordem.CONSTRUIR || h.profissao == Profissao.CONSTRUTOR) {
                if (temObra) {
                    h.tarefa = 2;
                    obra100 += eficiencia(h);
                    gastarEnergia(h);
                } else {
                    h.tarefa = 5;
                    h.energia = Math.min(100, h.energia + 5);
                }
            } else if (h.profissao.precisaVaga()) {
                int p = h.profissao.ordinal();
                if (vagasRest[p] > 0) {
                    vagasRest[p]--;
                    h.tarefa = 1;
                    int saida = h.profissao.producao() * eficiencia(h);
                    switch (h.profissao) {
                        case AGRICULTOR -> comida100 += saida;
                        case LENHADOR -> madeira100 += saida;
                        case MINEIRO -> pedra100 += saida;
                        default -> { }
                    }
                    gastarEnergia(h);
                } else {
                    h.tarefa = 4;
                }
            } else {
                h.tarefa = 0;
                comida100 += Profissao.LIVRE.producao() * eficiencia(h);
                gastarEnergia(h);
            }

            int alvo = (h.fome * 30 + moradiaSat * 25 + h.saude * 15 + h.energia * 10 + renda * 20) / 100;
            alvo = Math.min(100, alvo + d.bonusFeliz);
            if (h.felicidade < alvo) h.felicidade = Math.min(alvo, h.felicidade + 5);
            else if (h.felicidade > alvo) h.felicidade = Math.max(alvo, h.felicidade - 5);
        }

        for (Habitante m : mortos) {
            d.habitantes.remove(m);
            noticia(server, d, m.nome + " morreu de fome.");
        }

        // A tecnologia da era aumenta a produção
        int eraMult = 100 + 15 * d.era.ordinal();
        d.food = Math.min(d.armazem, d.food + comida100 * eraMult / 10000);
        d.wood = Math.min(d.armazem, d.wood + madeira100 * eraMult / 10000);
        d.stone = Math.min(d.armazem, d.stone + pedra100 * eraMult / 10000);

        if (obra100 > 0) {
            avancarObras(server, d, obra100);
        }
    }

    private static void avancarObras(MinecraftServer server, NationData d, int trabalho100) {
        ServerLevel level = server.overworld();
        int restante = trabalho100;
        for (Edificio e : d.edificios) {
            if (e.concluido) continue;
            int total = e.tipo.trabalho() * 100;
            int usa = Math.min(total - e.progresso, restante);
            e.progresso += usa;
            restante -= usa;
            boolean pronto = e.progresso >= total;

            if (level.isLoaded(e.pos)) {
                int blocos = Estruturas.gerar(e.tipo).size();
                Obras.colocarAte(level, e, pronto ? blocos : blocos * e.progresso / total);
            }
            if (pronto) {
                e.concluido = true;
                noticia(server, d, e.tipo.nome() + " concluída.");
            }
            if (restante <= 0) break;
        }
    }

    /** Termina de erguer estruturas cuja área não estava carregada quando a obra acabou. */
    private static void completarPendentes(ServerLevel level, NationData d) {
        for (Edificio e : d.edificios) {
            if (e.concluido && e.colocados < Estruturas.gerar(e.tipo).size() && level.isLoaded(e.pos)) {
                Obras.colocarAte(level, e, Integer.MAX_VALUE);
            }
        }
    }

    private static void fecharEconomia(NationData d) {
        int pop = d.habitantes.size();
        int empregados = 0;
        for (Habitante h : d.habitantes) {
            if (h.profissao != Profissao.LIVRE) empregados++;
        }
        int receita = pop * d.tax * d.mediaFeliz / 100 + d.renda;
        int salarios = empregados * d.salario;
        int manutencao = (d.concluidos + 1) / 2;
        d.lastIncome = receita;
        d.lastExpenses = salarios + manutencao;
        d.emeralds = Math.max(0, d.emeralds + receita - salarios - manutencao);
    }

    // ------------------------------------------------------------------- sync

    public static void enviarSync(ServerPlayer p, NationData d) {
        String aviso = AVISO.remove(p.getUUID());
        if (aviso == null) aviso = "";
        if (aviso.length() > 200) aviso = aviso.substring(0, 200);

        int total = Math.max(1, (d.habitantes.size() + POR_PAGINA - 1) / POR_PAGINA);
        int pagina = Math.max(0, Math.min(PAGINA.getOrDefault(p.getUUID(), 0), total - 1));
        int ini = pagina * POR_PAGINA;
        int fim = Math.min(d.habitantes.size(), ini + POR_PAGINA);
        List<SyncPayload.HabLinha> linhas = new ArrayList<>();
        for (int i = ini; i < fim; i++) {
            Habitante h = d.habitantes.get(i);
            linhas.add(new SyncPayload.HabLinha(h.seq, h.nome, h.profissao.ordinal(), h.ordem.ordinal(),
                    h.tarefa, h.fome, h.energia, h.saude, h.felicidade, h.nivel()));
        }

        int projTipo = -1;
        int projPct = 0;
        for (Edificio e : d.edificios) {
            if (!e.concluido) {
                projTipo = e.tipo.ordinal();
                projPct = e.progresso / Math.max(1, e.tipo.trabalho());
                break;
            }
        }

        PacketDistributor.sendToPlayer(p, new SyncPayload(
                d.nome, d.era.ordinal(), d.emeralds, d.food, d.wood, d.stone, d.armazem,
                d.habitantes.size(), d.era.populacaoMaxima(), d.moradia,
                d.mediaFome, d.mediaSaude, d.mediaFeliz, d.tax, d.salario, d.lastIncome, d.lastExpenses,
                d.contagem.clone(), d.vagas.clone(), d.edConcluidos.clone(), d.edObra.clone(),
                projTipo, projPct, pagina, total, linhas, noModoPrefeito(p), aviso,
                new ArrayList<>(d.historico)));
    }

    public static void abrirPainel(ServerPlayer p, NationData d) {
        calcularDerivados(d);
        enviarSync(p, d);
        PacketDistributor.sendToPlayer(p, new ScreenPayload(1));
    }

    // ------------------------------------------------------------- fundação

    /** Primeiro clique no bloco: pede ao cliente para abrir a tela de configurações. */
    public static void pedirConfiguracao(ServerPlayer p, BlockPos pos) {
        PENDENTE.put(p.getUUID(), pos.immutable());
        PacketDistributor.sendToPlayer(p, new ScreenPayload(0));
    }

    private static Edificio novoEdificio(NationData d, TipoEdificio tipo, BlockPos pos, boolean concluido) {
        Edificio e = new Edificio();
        e.id = d.proxEdId++;
        e.tipo = tipo;
        e.pos = pos;
        e.concluido = concluido;
        e.progresso = concluido ? tipo.trabalho() * 100 : 0;
        d.edificios.add(e);
        return e;
    }

    private static Habitante novoHabitante(NationData d, RandomSource r, Profissao prof) {
        Habitante h = new Habitante();
        h.seq = d.proxSeq++;
        h.nome = NOMES[r.nextInt(NOMES.length)];
        h.idade = 18 + r.nextInt(25);
        h.profissao = prof;
        return h;
    }

    /** Constrói já pronto um edifício inicial ao redor do Centro. */
    private static void criarInicial(ServerLevel level, NationData d, TipoEdificio tipo, int dx, int dz) {
        BlockPos origem = new BlockPos(d.center.getX() + dx, d.center.getY() - 1, d.center.getZ() + dz);
        Edificio e = novoEdificio(d, tipo, origem, true);
        Obras.preparar(level, e);
        Obras.colocarAte(level, e, Integer.MAX_VALUE);
    }

    /** Desliga mecânicas do Minecraft que não fazem sentido numa vila administrada pelo jogador. */
    private static void aplicarRegras(MinecraftServer server) {
        GameRules regras = server.getGameRules();
        regras.getRule(GameRules.RULE_DISABLE_RAIDS).set(true, server);
        regras.getRule(GameRules.RULE_DOINSOMNIA).set(false, server);
        regras.getRule(GameRules.RULE_DO_PATROL_SPAWNING).set(false, server);
        regras.getRule(GameRules.RULE_DO_TRADER_SPAWNING).set(false, server);
    }

    /** Chamado quando o jogador aperta "Começar" na tela de configurações. */
    public static void iniciarNacao(ServerPlayer p, String nome, int villagers, int esmeraldas, int dificuldade) {
        BlockPos pos = PENDENTE.remove(p.getUUID());
        if (pos == null) return;

        NationData d = NationData.get(p.server);
        if (d.center != null) return;

        ServerLevel level = p.server.overworld();
        if (!(level.getBlockState(pos).getBlock() instanceof NationCenterBlock)) {
            msg(p, "O Centro da Nação não está mais lá. Coloque-o de novo e clique nele.");
            return;
        }

        villagers = Math.max(1, Math.min(10, villagers));
        esmeraldas = Math.max(0, Math.min(500, esmeraldas));
        dificuldade = Math.max(0, Math.min(2, dificuldade));

        String limpo = nome == null ? "" : nome.trim();
        if (limpo.length() > 24) limpo = limpo.substring(0, 24);
        if (limpo.isEmpty()) limpo = "Vila de " + p.getName().getString();

        d.nome = limpo;
        d.dono = p.getUUID();
        d.center = pos;
        d.era = Era.PRIMITIVA;
        d.food = 40;
        d.wood = 60;
        d.stone = 30;
        d.emeralds = esmeraldas;
        d.tax = 1;
        d.salario = 0;
        d.lastIncome = 0;
        d.lastExpenses = 0;
        d.dificuldade = dificuldade;
        d.ciclo = 0;
        d.proxSeq = 1;
        d.proxEdId = 1;
        d.visiveis = 0;
        d.habitantes.clear();
        d.edificios.clear();
        d.historico.clear();

        // Edifícios iniciais, já erguidos ao redor do Centro
        criarInicial(level, d, TipoEdificio.CABANA, 6, -2);
        criarInicial(level, d, TipoEdificio.FAZENDA, -14, -4);
        criarInicial(level, d, TipoEdificio.SERRARIA, -2, 8);

        Profissao[] ordem = { Profissao.AGRICULTOR, Profissao.LENHADOR, Profissao.CONSTRUTOR,
                Profissao.AGRICULTOR, Profissao.LENHADOR, Profissao.LIVRE };
        RandomSource r = level.getRandom();
        for (int i = 0; i < villagers; i++) {
            d.habitantes.add(novoHabitante(d, r, ordem[i % ordem.length]));
        }
        d.visiveis = spawnarVillagers(level, d, Math.min(villagers, MAX_VISIVEIS));

        aplicarRegras(p.server);
        registrar(d, "Vila fundada: " + limpo);
        d.setDirty();

        msg(p, limpo + " foi fundada! Construa com [B] mirando no chão, abra o painel com [N].");
        entrarModoPrefeito(p, d, true);
        abrirPainel(p, d);
    }

    // ------------------------------------------------------------ recrutador

    private static int spawnarVillagers(ServerLevel level, NationData d, int quantidade) {
        if (quantidade <= 0 || !level.isLoaded(d.center)) return 0;
        int criados = 0;
        RandomSource r = level.getRandom();
        for (int i = 0; i < quantidade; i++) {
            BlockPos base = d.center.offset(r.nextInt(5) - 2, 0, r.nextInt(5) - 2);
            BlockPos topo = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, base);
            Villager v = EntityType.VILLAGER.spawn(level, topo, MobSpawnType.MOB_SUMMONED);
            if (v != null) criados++;
        }
        return criados;
    }

    public static void recrutar(ServerLevel level, Player player, NationData d, int quantidade) {
        int maximo = d.era.populacaoMaxima();
        if (d.habitantes.size() + quantidade > maximo) {
            msg(player, "A era " + d.era.nome() + " comporta " + maximo + " habitantes (agora: "
                    + d.habitantes.size() + "). Avance de era para crescer.");
            return;
        }
        int custo = quantidade * CUSTO_RECRUTA;
        if (d.emeralds < custo) {
            msg(player, "Recrutar " + quantidade + " custa " + custo + " esmeraldas. Você tem " + d.emeralds + ".");
            return;
        }
        d.emeralds -= custo;
        RandomSource r = level.getRandom();
        for (int i = 0; i < quantidade; i++) {
            d.habitantes.add(novoHabitante(d, r, Profissao.LIVRE));
        }
        int visiveisNovos = Math.min(quantidade, MAX_VISIVEIS - d.visiveis);
        d.visiveis += spawnarVillagers(level, d, visiveisNovos);
        d.setDirty();
        msg(player, quantidade + " habitante(s) recrutado(s). Designe profissões no painel.");
    }

    // ----------------------------------------------------------------- ações

    public static void executarAcao(ServerPlayer p, int acao, int valor) {
        NationData d = NationData.get(p.server);
        if (d.center == null) return;
        calcularDerivados(d);

        switch (acao) {
            case ActionPayload.PROF_MAIS -> atribuir(p, d, valor);
            case ActionPayload.PROF_MENOS -> liberar(d, valor);
            case ActionPayload.IMPOSTO_MAIS -> definirImposto(p, d, d.tax + 1);
            case ActionPayload.IMPOSTO_MENOS -> definirImposto(p, d, d.tax - 1);
            case ActionPayload.SALARIO_MAIS -> definirSalario(p, d, d.salario + 1);
            case ActionPayload.SALARIO_MENOS -> definirSalario(p, d, d.salario - 1);
            case ActionPayload.RECRUTAR -> recrutar(p.server.overworld(), p, d, Math.max(1, Math.min(10, valor)));
            case ActionPayload.AVANCAR -> tentarAvancar(p, d);
            case ActionPayload.CONSTRUIR_AQUI -> construirAqui(p, d, valor);
            case ActionPayload.CANCELAR_OBRA -> cancelarObra(p, d);
            case ActionPayload.HAB_PROF -> {
                Habitante h = porSeq(d, valor);
                if (h != null) {
                    h.profissao = h.profissao.proxima();
                    d.setDirty();
                }
            }
            case ActionPayload.HAB_ORDEM -> {
                Habitante h = porSeq(d, valor);
                if (h != null) {
                    h.ordem = h.ordem.proxima();
                    d.setDirty();
                }
            }
            case ActionPayload.PAGINA -> PAGINA.put(p.getUUID(), Math.max(0, valor));
            default -> { }
        }
        calcularDerivados(d);
        enviarSync(p, d);
    }

    private static Habitante porSeq(NationData d, int seq) {
        for (Habitante h : d.habitantes) {
            if (h.seq == seq) return h;
        }
        return null;
    }

    private static void atribuir(Player p, NationData d, int prof) {
        Profissao alvo = Profissao.porIndice(prof);
        if (alvo == Profissao.LIVRE) return;
        for (Habitante h : d.habitantes) {
            if (h.profissao == Profissao.LIVRE) {
                h.profissao = alvo;
                d.setDirty();
                return;
            }
        }
        msg(p, "Nenhum coletor livre. Recrute mais habitantes ou libere alguém de outra profissão.");
    }

    private static void liberar(NationData d, int prof) {
        Profissao alvo = Profissao.porIndice(prof);
        for (int i = d.habitantes.size() - 1; i >= 0; i--) {
            Habitante h = d.habitantes.get(i);
            if (h.profissao == alvo && alvo != Profissao.LIVRE) {
                h.profissao = Profissao.LIVRE;
                d.setDirty();
                return;
            }
        }
    }

    public static void definirImposto(Player player, NationData d, int valor) {
        d.tax = Math.max(0, Math.min(IMPOSTO_MAX, valor));
        d.setDirty();
        msg(player, "Imposto: " + d.tax + " esmeralda(s) por habitante por minuto. Imposto alto reduz a felicidade.");
    }

    public static void definirSalario(Player player, NationData d, int valor) {
        d.salario = Math.max(0, Math.min(SALARIO_MAX, valor));
        d.setDirty();
        msg(player, "Salário: " + d.salario + " esmeralda(s) por empregado por minuto. Salário alto aumenta a felicidade.");
    }

    /** Posiciona o edifício onde o jogador está mirando: marca o terreno e enfileira a obra. */
    private static void construirAqui(ServerPlayer p, NationData d, int tipoIdx) {
        TipoEdificio t = TipoEdificio.porIndice(tipoIdx);
        if (t.eraMinima() > d.era.ordinal()) {
            msg(p, t.nome() + " exige a era " + Era.porIndice(t.eraMinima()).nome() + ".");
            return;
        }
        if (p.level().dimension() != Level.OVERWORLD) {
            msg(p, "As construções da vila só podem ser feitas no Overworld.");
            return;
        }
        int fila = 0;
        for (int n : d.edObra) fila += n;
        if (fila >= MAX_FILA) {
            msg(p, "A fila de obras está cheia (" + MAX_FILA + "). Espere ou cancele uma obra.");
            return;
        }
        if (d.wood < t.madeira() || d.stone < t.pedra()) {
            msg(p, t.nome() + " custa " + t.madeira() + " de madeira e " + t.pedra() + " de pedra. Faltam materiais.");
            return;
        }

        HitResult alvoMira = p.pick(200.0, 1.0F, false);
        if (alvoMira.getType() != HitResult.Type.BLOCK) {
            msg(p, "Mire no chão onde quer construir e aperte B.");
            return;
        }
        BlockPos chao = ((BlockHitResult) alvoMira).getBlockPos();
        BlockPos origem = new BlockPos(chao.getX() - t.largura() / 2, chao.getY(), chao.getZ() - t.profundidade() / 2);

        ServerLevel level = p.server.overworld();
        String erro = Obras.validar(level, d, t, origem);
        if (erro != null) {
            msg(p, erro);
            return;
        }

        d.wood -= t.madeira();
        d.stone -= t.pedra();
        Edificio e = novoEdificio(d, t, origem, false);
        Obras.preparar(level, e);
        d.setDirty();
        msg(p, t.nome() + " marcada no terreno. Os construtores vão erguer a obra.");
    }

    private static void cancelarObra(ServerPlayer p, NationData d) {
        Edificio alvo = null;
        for (int i = d.edificios.size() - 1; i >= 0; i--) {
            if (!d.edificios.get(i).concluido) {
                alvo = d.edificios.get(i);
                break;
            }
        }
        if (alvo == null) {
            msg(p, "Não há obras para cancelar.");
            return;
        }
        int pct = alvo.progresso == 0 ? 100 : 50;
        d.wood = Math.min(d.armazem, d.wood + alvo.tipo.madeira() * pct / 100);
        d.stone = Math.min(d.armazem, d.stone + alvo.tipo.pedra() * pct / 100);
        Obras.demolir(p.server.overworld(), alvo);
        d.edificios.remove(alvo);
        d.setDirty();
        msg(p, "Obra cancelada: " + alvo.tipo.nome() + " (materiais devolvidos: " + pct + "%).");
    }

    public static void tentarAvancar(Player player, NationData d) {
        calcularDerivados(d);
        Era atual = d.era;
        if (!atual.temProxima()) {
            msg(player, "Sua civilização já está na era Futurista.");
            return;
        }
        int custo = atual.custo();
        int custoEsm = atual.custoEsmeraldas();
        int pop = d.habitantes.size();

        if (pop < atual.populacaoMinima()) {
            msg(player, "Faltam habitantes: você tem " + pop + " e precisa de " + atual.populacaoMinima() + ".");
            return;
        }
        if (d.concluidos < atual.edificiosMinimos()) {
            msg(player, "Faltam edifícios concluídos: você tem " + d.concluidos + " e precisa de " + atual.edificiosMinimos() + ".");
            return;
        }
        if (d.wood < custo || d.stone < custo) {
            msg(player, "Precisa de " + custo + " de madeira e " + custo + " de pedra. Você tem " + d.wood + " e " + d.stone + ".");
            return;
        }
        if (d.emeralds < custoEsm) {
            msg(player, "Faltam esmeraldas: precisa de " + custoEsm + " e o tesouro tem " + d.emeralds + ".");
            return;
        }

        d.wood -= custo;
        d.stone -= custo;
        d.emeralds -= custoEsm;
        d.era = atual.proxima();
        d.setDirty();

        if (player instanceof ServerPlayer sp) {
            noticia(sp.server, d, d.nome + " avançou para a era " + d.era.nome() + "!");
        }
    }

    // ----------------------------------------------------------------- status

    public static void mostrarStatus(Player player, NationData d) {
        calcularDerivados(d);
        msg(player, "=== " + d.nome + " - Era " + d.era.nome() + " ===");
        msg(player, "Habitantes: " + d.habitantes.size() + "/" + d.era.populacaoMaxima() + " | Moradia: " + d.moradia);
        msg(player, "Esmeraldas: " + d.emeralds + " (último minuto: +" + d.lastIncome + " / -" + d.lastExpenses + ")");
        msg(player, "Comida: " + d.food + " | Madeira: " + d.wood + " | Pedra: " + d.stone + " (máx. " + d.armazem + ")");
        msg(player, "Fome: " + d.mediaFome + " | Saúde: " + d.mediaSaude + " | Felicidade: " + d.mediaFeliz
                + " (" + Rules.humor(d.mediaFeliz) + ")");
    }

    // ---------------------------------------------------------- Modo Prefeito

    /** O Modo Prefeito é o modo espectador enquanto existe uma vila. */
    public static boolean noModoPrefeito(ServerPlayer player) {
        return player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR;
    }

    /** Coloca o jogador no Modo Prefeito, opcionalmente com a câmera alta olhando para a vila. */
    public static void entrarModoPrefeito(ServerPlayer player, NationData d, boolean camera) {
        if (!noModoPrefeito(player)) {
            player.setGameMode(GameType.SPECTATOR);
        }
        if (camera && d.center != null && player.level().dimension() == Level.OVERWORLD) {
            player.connection.teleport(d.center.getX() + 0.5, d.center.getY() + 30.0, d.center.getZ() + 30.5,
                    180.0F, 45.0F);
        }
    }

    /** Saída de emergência (comando /nacao sair): volta ao modo sobrevivência. */
    public static void sairModoPrefeito(ServerPlayer player, NationData d) {
        if (!noModoPrefeito(player)) {
            msg(player, "Você não está no Modo Prefeito.");
            return;
        }
        player.setGameMode(GameType.SURVIVAL);
        if (d.center != null && player.level().dimension() == Level.OVERWORLD) {
            player.teleportTo(d.center.getX() + 0.5, d.center.getY() + 1.0, d.center.getZ() + 0.5);
        }
        msg(player, "Modo Prefeito encerrado (modo sobrevivência).");
    }
}
