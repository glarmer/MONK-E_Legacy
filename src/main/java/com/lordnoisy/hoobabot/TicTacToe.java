package com.lordnoisy.hoobabot;

import com.lordnoisy.hoobabot.utility.DiscordUtilities;
import com.lordnoisy.hoobabot.utility.EmbedBuilder;
import com.lordnoisy.hoobabot.utility.Utilities;
import discord4j.common.util.Snowflake;
import discord4j.core.object.Embed;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.core.spec.MessageEditSpec;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TicTacToe {
    private final String fighterSymbol = "\u274E";
    private final String opponentSymbol = "\uD83C\uDD7E";
    private final String emptySymbol = ":black_large_square:";

    /**
     * Start a game of tic-tac-toe
     * @param fighter the person who initiated the game
     * @param opponent the opponent
     * @return the message to be created
     */
    public MessageCreateSpec startTicTacToe(Member fighter, Snowflake opponent) {
        String name = fighter.getDisplayName();
        String url = fighter.getAvatarUrl();
        String[][] moves = new String[3][3];
        String startingBoard = createDescription(moves);
        return MessageCreateSpec.builder()
                .addEmbed(createTicTacToeEmbed(name, url, startingBoard, opponent.asString(), opponent.asString(), false))
                .addAllComponents(createButtonRows(fighter.getId().asString(), opponent.asString(), "1", null, moves, false))
                .build();
    }

    public MessageEditSpec updateTicTacToe(Message message, String buttonId, String buttonPresserId) {
        if (!DiscordUtilities.isBotMessage(message.getClient(), message)) {
            System.out.println("TIC TAC TOE BOT");
            return null;
        }
        if (message.getEmbeds().size() != 1) {
            System.out.println("TIC TAC TOE EMBED");
            return null;
        }
        Embed ticTacToeEmbed = message.getEmbeds().get(0);
        Embed.Author author = ticTacToeEmbed.getAuthor().orElse(null);
        if (author == null) {
            System.out.println("TIC TAC TOE AUTHOR");
            return null;
        }
        String authorName = author.getName().orElse("");
        if (!authorName.contains("tic-tac-toe")) {
            System.out.println("TIC TAC TOE AUTHOR NAME");
            return null;
        }
        String name = authorName.split("has")[0].trim();
        String url = author.getUrl().orElse("");

        String[] buttonInfo = buttonId.split(":");

        //Make sure only the allowed user is making a turn
        String currentTurnUser = buttonInfo[2];
        String nextTurnUser = buttonInfo[3];
        if (!currentTurnUser.equals(buttonPresserId)) {
            System.out.println("TIC TAC TOE CURRENT TURN USER " + currentTurnUser + " " + buttonPresserId);
            return null;
        }


        String[][] moves = new String[3][3];

        //Add freshly pressed option
        int newX = Integer.parseInt(buttonInfo[1].split(",")[0])-1;
        int newY = Integer.parseInt(buttonInfo[1].split(",")[1])-1;
        String symbol;
        int turnNumber = Integer.parseInt(buttonInfo[4]);
        if (turnNumber%2==1) {
            symbol = fighterSymbol;
        } else {
            symbol = opponentSymbol;
        }
        moves[newX][newY] = symbol;

        // Calculate board
        String boardState = "";
        for (int i = 5; i < buttonInfo.length; i++) {
            int currentIteration = i-5;
            int y;
            int x;
            if (currentIteration == 0) {
                y = 0;
                x = 0;
            } else {
                y = Math.floorDiv(currentIteration, 3);
                x = currentIteration - (y*3);
                System.out.println("TIC TAC TOE CURRENT PLACE " + x + " " + y + " " + currentIteration);
            }
            if (newX == x && newY == y) {
                if (symbol.equals(fighterSymbol)) {
                    boardState = boardState.concat("x:");
                } else {
                    boardState = boardState.concat("o:");
                }
                continue;
            }
            String currentPlace = buttonInfo[i];
            System.out.println("TIC TAC TOE CURRENT PLACE " + currentPlace);
            switch (currentPlace) {
                case "e":
                    boardState = boardState.concat("e:");
                    break;
                case "x":
                    boardState = boardState.concat("x:");
                    moves[x][y] = fighterSymbol;
                    break;
                case "o":
                        boardState = boardState.concat("o:");
                        moves[x][y] = opponentSymbol;
                    break;
            }
        }
        System.out.println("TIC TAC TOE CURRENT PLACE " + boardState);

        boolean hasWon = hasWon(moves);

        String opponentId = ticTacToeEmbed.getDescription().orElse("b\nn").split("challenged")[1].replace("@","").replace("<","").replace(">","").split("!")[0].trim();

        String playBoard = createDescription(moves);
        List<EmbedCreateSpec> embeds = List.of(createTicTacToeEmbed(name, url, playBoard, opponentId, currentTurnUser, hasWon));
        List<LayoutComponent> buttons = createButtonRows(nextTurnUser, currentTurnUser, String.valueOf(turnNumber+1), boardState, moves, hasWon);

        return MessageEditSpec.builder()
                .addAllEmbeds(embeds)
                .addAllComponents(buttons)
                .build();
    }

    /**
     * Calculates if a user has won
     * @param moves the array of current moves
     * @return
     */
    public boolean hasWon(String[][] moves) {
        for (int xy = 0; xy < 3; xy++) {
            //Check all columns
            if (!Objects.equals(moves[xy][0], null)) {
                if (Objects.equals(moves[xy][0], moves[xy][1]) && Objects.equals(moves[xy][0], moves[xy][2])) {
                    return true;
                }
            }
            if (!Objects.equals(moves[0][xy], null)) {
                if (Objects.equals(moves[0][xy], moves[1][xy]) && Objects.equals(moves[0][xy], moves[2][xy])) {
                    return true;
                }
            }
        }
        if (moves[1][1]!=null) {
            if (Objects.equals(moves[1][1], moves[0][0]) && Objects.equals(moves[1][1], moves[2][2])) {
                return true;
            }
            if (Objects.equals(moves[1][1], moves[2][0]) && Objects.equals(moves[1][1], moves[0][2])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates the tic-tac-toe embed
     * @param name the challengers name
     * @param url the challengers profile picture url
     * @param currentBoard the current state of the board
     * @return the embed
     */
    public EmbedCreateSpec createTicTacToeEmbed(String name, String url, String currentBoard, String opponentId, String currentUser, boolean hasWon) {
        String title = name + " has initiated tic-tac-toe!";
        String extraDescription;
        if (hasWon) {
            extraDescription = "<@" + currentUser + "> has won the game, congratulations!\n";
        } else {
            extraDescription = "You have been challenged <@" + opponentId + ">!\n";
        }
        return EmbedCreateSpec.builder()
                .author(title, url, url)
                .description(extraDescription + currentBoard)
                .color(EmbedBuilder.getStandardColor())
                .timestamp(Instant.now())
                .footer(EmbedBuilder.getFooterText(), (EmbedBuilder.getFooterIconURL() + String.valueOf(Utilities.getRandomNumber(0,156)) + ".png"))
                .build();
    }

    /**
     * Turn a moves array into a valid board
     * @param moves the 2d array of moves
     * @return a valid board
     */
    public String createDescription(String[][] moves) {
        String description = "";
        for (int y = 0; y <= 2; y++) {
            for (int x = 0; x <= 2; x++) {
                if (moves[x][y]==null) {
                    description = description.concat(":black_large_square:");
                } else {
                    description = description.concat(moves[x][y]);
                }
            }
            description = description.concat("\n");
        }
        return description;
    }

    /**
     * Create all the buttons for tic-tac-toe
     * @param currentTurnUser the user who's taking their turn
     * @param nextTurnUser the user whose turn it isn't
     * @return all tic-tac-toe buttons in a List of ActionRows
     */
    public List<LayoutComponent> createButtonRows (String currentTurnUser, String nextTurnUser, String turnNumber, String boardState, String[][] moves, boolean isFinished) {
        ArrayList<LayoutComponent> buttons = new ArrayList<>();
        for (int y = 1; y <= 3; y++) {
            ArrayList<Button> tempButtonRow = new ArrayList<>();
            for (int x = 1; x <= 3; x++) {
                boolean isDisabled = moves[x - 1][y - 1] != null;
                if (isFinished) {
                    isDisabled = true;
                }
                Button newButton = this.createButton(x, y, currentTurnUser, nextTurnUser, turnNumber, isDisabled, boardState);
                tempButtonRow.add(newButton);
            }
            ActionRow actionRow = ActionRow.of(tempButtonRow);
            buttons.add(actionRow);
        }
        return buttons;
    }

    /**
     * Create a tic-tac-toe button
     * @param x the x coordinate the button relates to
     * @param y the y coordinate the button relates to
     * @param currentTurnUser the user who's taking their turn
     * @param nextTurnUser the user whose turn it isn't
     * @param isDisabled if a button is disabled
     * @return a valid tic-tac-toe button
     */
    public Button createButton (int x, int y, String currentTurnUser, String nextTurnUser, String turnNumber, boolean isDisabled, String boardState) {
        if (boardState==null) {
            boardState = "e:e:e:e:e:e:e:e:e";
        }
        return Button.primary("tic_tac_toe:"+x+","+y+":"+currentTurnUser+":"+nextTurnUser+":"+turnNumber+":"+boardState, "Here!").disabled(isDisabled);
    }
}