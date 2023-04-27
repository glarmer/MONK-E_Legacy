package com.lordnoisy.hoobabot.utility;

import com.lordnoisy.hoobabot.Poll;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.reaction.Reaction;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.rest.util.Permission;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;

public class DiscordUtilities {
    private static final String crossReact = "\u274c";
    public static Boolean validatePermissions(Mono<Member> author) {
        AtomicBoolean atomicBoolean = new AtomicBoolean(false);
        author.flatMap(Member::getBasePermissions)
                .map(permissions -> {
                    if(permissions.contains(Permission.ADMINISTRATOR)){
                        atomicBoolean.set(true);
                    }
                    return permissions.contains(Permission.ADMINISTRATOR);
                }).subscribe().dispose();
        return atomicBoolean.get();
    }

    public static Boolean validateBotOwner(Member author) {
        return author.getId().asString().equals("359000351455838219");
    }

    public static Button deleteButton (Snowflake id) {
        return Button.danger("delete:"+id.asString(), "X");
    }

    public static Mono<Boolean> deleteMessage(Message message, Snowflake buttonUserId, String authorId) {
        if (buttonUserId.asString().equals(authorId)) {
            return message.delete().thenReturn(true);
        } else {
            return Mono.just(false);
        }
    }

    public static boolean isBotMessage(GatewayDiscordClient gateway, Message message) {
        Snowflake botID = gateway.getSelfId();
        return botID.equals(message.getAuthor().get().getId());
    }

    public static void deleteMessage(MessageCreateEvent event, GatewayDiscordClient gateway){
        try {
            if (validateBotOwner(event.getMember().get())) {
                String[] messageSplit = event.getMessage().getContent().split(" ");
                String channel_id = event.getMessage().getContent().split(" ")[1];
                for (int i = 2; i < messageSplit.length; i++) {
                    gateway.getMessageById(Snowflake.of(channel_id), Snowflake.of(messageSplit[i])).flatMap(message -> message.delete()).subscribe();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
