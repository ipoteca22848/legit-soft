package com.example.auravisuals.module.misc;

import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.StringSetting;
import com.example.auravisuals.util.server.FunTimeUtil;

public class ClanInvest extends Module {
    private final StringSetting threshold = new StringSetting("Порог монет", "1кк");
    private final StringSetting amount = new StringSetting("Сумма вложения", "500к");
    private long lastInvestCheckTime = 0L;

    public ClanInvest() {
        super("ClanInvest", "Автоматически инвест монет", Category.MISC);
        this.addSettings(this.threshold, this.amount);
    }

    @Override
    public void onDisable() {
        this.lastInvestCheckTime = 0L;
    }

    @Override
    public void onTick() {
        if (this.mc.player != null && this.mc.world != null && this.mc.getNetworkHandler() != null && FunTimeUtil.isFunTimeServer(this.mc)) {
            if (System.currentTimeMillis() - this.lastInvestCheckTime >= 5000L) {
                double balance = FunTimeUtil.getPlayerBalance(this.mc);
                long investAmount;
                if (balance != -1.0
                    && balance > FunTimeUtil.parseSuffixedValue(this.threshold.getValue())
                    && (investAmount = FunTimeUtil.parseSuffixedValue(this.amount.getValue())) > 0L) {
                    this.mc.getNetworkHandler().sendChatCommand("clan invest " + investAmount);
                }

                this.lastInvestCheckTime = System.currentTimeMillis();
            }
        }
    }
}
