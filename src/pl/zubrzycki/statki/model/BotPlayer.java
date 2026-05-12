package pl.zubrzycki.statki.model;

public class BotPlayer extends Player {

    private final BotAI ai;

    public BotPlayer(String name, BotDifficulty diff) {
        super(name);
        this.ai = new BotAI(diff);
    }

    public BotAI getAI() {
        return ai;
    }
}
