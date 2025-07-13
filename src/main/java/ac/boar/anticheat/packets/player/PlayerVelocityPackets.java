package ac.boar.anticheat.packets.player;

import ac.boar.anticheat.data.input.VelocityData;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.util.math.Vec3;
import ac.boar.protocol.event.CloudburstPacketEvent;
import ac.boar.protocol.listener.PacketListener;
import org.cloudburstmc.protocol.bedrock.data.MovementEffectType;
import org.cloudburstmc.protocol.bedrock.packet.MovementEffectPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket;

public class PlayerVelocityPackets implements PacketListener {
    @Override
    public void onPacketSend(final CloudburstPacketEvent event, final boolean immediate) {
        final BoarPlayer player = event.getPlayer();

        // Yes only this, there no packet for explosion (for bedrock), geyser translate explosion directly to SetEntityMotionPacket
        if (event.getPacket() instanceof SetEntityMotionPacket packet) {
            if (packet.getRuntimeEntityId() != player.runtimeEntityId) {
                return;
            }

            player.sendLatencyStack(immediate);
            player.queuedVelocities.put(player.sentStackId.get() + 1, new VelocityData(player.sentStackId.get() + 1, player.tick, new Vec3(packet.getMotion())));
            event.getPostTasks().add(player::sendLatencyStack);
        }

        if (event.getPacket() instanceof MovementEffectPacket packet) {
            if (packet.getEntityRuntimeId() != player.runtimeEntityId || packet.getEffectType() != MovementEffectType.GLIDE_BOOST) {
                return;
            }

            // If you have rewind history that is not 0 and send tick id 0 this will fucked up the movement~~~:tm:
            // Well anyway.... if you just send a valid tick id or send an invalid id it works fine :D
            packet.setTick(Integer.MIN_VALUE);

            player.sendLatencyStack(immediate);
            player.getLatencyUtil().addTaskToQueue(player.sentStackId.get(), () -> {
                if (player.glideBoostTicks == 0 && packet.getDuration() == 0 || packet.getDuration() == Integer.MAX_VALUE) {
                    player.glideBoostTicks = 1;
                    return;
                }

                player.glideBoostTicks = packet.getDuration() / 2;
            });
        }
    }
}
