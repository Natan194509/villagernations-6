package com.example.villagernations;

import com.example.villagernations.client.ClientPayloadHandler;
import com.example.villagernations.net.ActionPayload;
import com.example.villagernations.net.ScreenPayload;
import com.example.villagernations.net.ServerPayloadHandler;
import com.example.villagernations.net.StartPayload;
import com.example.villagernations.net.SyncPayload;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(NationsMod.MODID)
public class NationsMod {
    public static final String MODID = "villagernations";

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    public static final DeferredBlock<NationCenterBlock> NATION_CENTER = BLOCKS.register(
            "nation_center",
            () -> new NationCenterBlock(BlockBehaviour.Properties.of()
                    .strength(3.0f)
                    .sound(SoundType.STONE)));

    public static final DeferredItem<BlockItem> NATION_CENTER_ITEM =
            ITEMS.registerSimpleBlockItem("nation_center", NATION_CENTER);

    public NationsMod(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener(this::addToCreativeTab);
        modBus.addListener(this::registrarPayloads);
        NeoForge.EVENT_BUS.addListener(NationLogic::onServerTick);
        NeoForge.EVENT_BUS.addListener(NationCommands::registrar);
        NeoForge.EVENT_BUS.addListener(NationLogic::onLogin);
        NeoForge.EVENT_BUS.addListener(NationLogic::onBaby);
    }

    private void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(NATION_CENTER_ITEM);
        }
    }

    private void registrarPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        // Lambdas: a classe do cliente só é carregada quando o pacote chega.
        registrar.playToClient(SyncPayload.TYPE, SyncPayload.CODEC,
                (payload, ctx) -> ClientPayloadHandler.sync(payload, ctx));
        registrar.playToClient(ScreenPayload.TYPE, ScreenPayload.CODEC,
                (payload, ctx) -> ClientPayloadHandler.tela(payload, ctx));
        registrar.playToServer(ActionPayload.TYPE, ActionPayload.CODEC, ServerPayloadHandler::acao);
        registrar.playToServer(StartPayload.TYPE, StartPayload.CODEC, ServerPayloadHandler::iniciar);
    }
}
