@file:Suppress("SameParameterValue", "RegExpRedundantEscape", "SpellCheckingInspection", "Unused")

package commands.utility

import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal
import net.dv8tion.jda.api.utils.FileUpload
import java.awt.*
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.net.URL
import java.nio.file.Files
import javax.imageio.ImageIO

class SnapCode : ListenerAdapter() {

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name != "snapcode") return
        when (event.channelType) {
            ChannelType.TEXT -> {
                val codeInput = TextInput.create("code_input", "Paste Your Code", TextInputStyle.PARAGRAPH)
                    .setPlaceholder("Insert your code here...")
                    .setRequired(true)
                    .setMaxLength(4000)
                    .build()
                val modal = Modal.create("codesnap_modal", "Generate Code Snapshot")
                    .addActionRow(codeInput)
                    .build()
                event.replyModal(modal).queue()
            }
            ChannelType.PRIVATE -> {
                val codeInput = TextInput.create("code_input", "Paste Your Code", TextInputStyle.PARAGRAPH)
                    .setPlaceholder("Insert your code here...")
                    .setRequired(true)
                    .setMaxLength(4000)
                    .build()
                val modal = Modal.create("codesnap_modal", "Generate Code Snapshot (DM)")
                    .addActionRow(codeInput)
                    .build()
                event.replyModal(modal).queue()
            }
            else -> {
                event.reply("❌ Questo comando non è supportato qui.").setEphemeral(true).queue()
            }
        }
    }
    override fun onModalInteraction(event: ModalInteractionEvent) {
        if (event.modalId != "codesnap_modal") return
        val code = event.getValue("code_input")?.asString ?: return
        if (code.isBlank()) {
            event.reply("❌ Code cannot be empty.").setEphemeral(true).queue()
            return
        }
        event.deferReply().queue()
        try {
            val language = detectLanguage(code)
            val imageBytes = generateImageFromCode(code, language)
            val tempFile = Files.createTempFile("code_snapshot", ".png")
            Files.write(tempFile, imageBytes)
            event.hook.sendFiles(FileUpload.fromData(tempFile.toFile(), "code_snapshot.png")).queue(
                { Files.deleteIfExists(tempFile) },
                { event.hook.sendMessage("❌ Failed to generate the code snapshot. Please try again.").setEphemeral(true).queue() }
            )
        } catch (e: Exception) {
            e.printStackTrace()
            event.hook.sendMessage("❌ Failed to generate the code snapshot. Please try again.").setEphemeral(true).queue()
        }
    }
    private fun generateImageFromCode(code: String, language: String): ByteArray {
        val lines = code.split("\n")
        val baseFontSize = 18
        val font = Font("Roboto", Font.PLAIN, baseFontSize)
        val padding = 20
        val buttonSize = 15
        val buttonPadding = 10
        val borderRadius = 20
        val fm = getFontMetrics(font)
        val maxLineWidth = lines.maxOfOrNull { fm.stringWidth(it) } ?: 0
        val imageWidth = maxLineWidth + padding * 2
        val imageHeight = fm.height * lines.size + padding * 3 + buttonSize + buttonPadding
        val image = BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        graphics.color = Color(30, 30, 46)
        graphics.fill(RoundRectangle2D.Double(0.0, 0.0, imageWidth.toDouble(), imageHeight.toDouble(), borderRadius.toDouble(), borderRadius.toDouble()))
        graphics.color = Color(255, 85, 85)
        graphics.fillOval(padding, padding / 2, buttonSize, buttonSize)
        graphics.color = Color(255, 211, 77)
        graphics.fillOval(padding + buttonSize + buttonPadding, padding / 2, buttonSize, buttonSize)
        graphics.color = Color(79, 223, 128)
        graphics.fillOval(padding + 2 * (buttonSize + buttonPadding), padding / 2, buttonSize, buttonSize)
        graphics.font = Font("Roboto", Font.BOLD, 14)
        graphics.color = Color(166, 227, 161)
        val languageLabelWidth = fm.stringWidth(language)
        graphics.drawString(language, imageWidth - languageLabelWidth - padding, padding + fm.ascent)
        graphics.font = font
        var y = padding * 2 + buttonSize
        for (line in lines) {
            drawHighlightedLine(graphics, line, padding, y, fm)
            y += fm.height
        }
        val logo = ImageIO.read(URL("https://i.imgur.com/budPftW.png"))
        val logoWidth = 30
        val logoHeight = 30
        val logoX = imageWidth - padding - logoWidth
        val logoY = imageHeight - padding - logoHeight - 5
        graphics.drawImage(logo, logoX, logoY, logoWidth, logoHeight, null)
        graphics.font = Font("Roboto", Font.BOLD, 9)
        val nekoTextLines = listOf("Generated by", "   Neko-CLI")
        val lineHeight = graphics.fontMetrics.height
        var textY = logoY + logoHeight + lineHeight - 5
        graphics.color = Color(91, 157, 243)
        for (line in nekoTextLines) {
            val textWidth = graphics.fontMetrics.stringWidth(line)
            val textX = logoX + (logoWidth - textWidth) / 2
            graphics.drawString(line, textX, textY)
            textY += lineHeight
        }
        graphics.dispose()
        val outputStream = ByteArrayOutputStream()
        ImageIO.write(image, "png", outputStream)
        return outputStream.toByteArray()
    }
    private fun drawHighlightedLine(graphics: Graphics2D, line: String, x: Int, y: Int, fm: FontMetrics) {
        val tokens = tokenize(line)
        var currentX = x
        for (token in tokens) {
            graphics.color = when (token.type) {
                TokenType.KEYWORD -> Color(205, 130, 186) // Mauve
                TokenType.STRING -> Color(245, 189, 230) // Pink
                TokenType.COMMENT -> Color(147, 153, 178) // Flamingo
                TokenType.NUMBER -> Color(245, 194, 231) // Lavender
                TokenType.FUNCTION -> Color(125, 196, 228) // Sapphire
                TokenType.OPERATOR -> Color(137, 221, 255) // Blue
                TokenType.VARIABLE -> Color(255, 203, 107) // Peach
                TokenType.TYPE -> Color(166, 218, 149) // Green
                TokenType.ANNOTATION -> Color(240, 198, 198) // Flamingo
                TokenType.CONSTANT -> Color(238, 212, 159) // Yellow
                TokenType.BASH_COMMENT -> Color(147, 153, 178) // Flamingo
                TokenType.HTML_TAG -> Color(125, 196, 228) // Sapphire
                TokenType.CSS_PROPERTY -> Color(166, 218, 149) // Green
                TokenType.SQL_KEYWORD -> Color(205, 130, 186) // Mauve
                TokenType.DEFAULT -> Color(202, 211, 245) // Text
            }
            graphics.drawString(token.text, currentX, y)
            currentX += fm.stringWidth(token.text)
        }
    }
    private fun tokenize(line: String): List<Token> {
        val tokens = mutableListOf<Token>()
        val regex = Regex(
            "\\b(if|else|for|while|return|class|fun|val|var|import|def|function|void|public|private|protected|static|final|const|abstract|interface|enum|SELECT|INSERT|UPDATE|DELETE|WHERE|JOIN)\\b|" +
                    "@\\w+|" +
                    "\\b(TRUE|FALSE|null|this|super)\\b|" +
                    "\"(.*?)\"|'(.*?)'|" +
                    "#.*|//.*|/\\*.*?\\*/|" +
                    "<\\/?[a-zA-Z][a-zA-Z0-9]*[^>]*>|" +
                    "\\b[a-zA-Z-]+(?=:\\b)" +
                    "\\b([0-9]+(\\.[0-9]+)?)\\b|" +
                    "\\b([a-zA-Z_][a-zA-Z0-9_]*)\\s*(?=\\()|" +
                    "[=+\\-*/<>!&|%^~]+"
        )
        var lastIndex = 0
        regex.findAll(line).forEach { matchResult ->
            if (matchResult.range.first > lastIndex) {
                tokens.add(Token(line.substring(lastIndex, matchResult.range.first), TokenType.DEFAULT))
            }
            when {
                matchResult.groups[1] != null -> tokens.add(Token(matchResult.value, TokenType.KEYWORD))
                matchResult.value.startsWith("@") -> tokens.add(Token(matchResult.value, TokenType.ANNOTATION))
                matchResult.groups[2] != null || matchResult.groups[3] != null -> tokens.add(Token(matchResult.value, TokenType.STRING))
                matchResult.value.startsWith("#") || matchResult.value.startsWith("//") || matchResult.value.startsWith("/*") -> tokens.add(Token(matchResult.value, TokenType.COMMENT))
                matchResult.value.matches("#.*".toRegex()) -> tokens.add(Token(matchResult.value, TokenType.BASH_COMMENT))
                matchResult.value.matches("<\\/?[a-zA-Z][a-zA-Z0-9]*[^>]*>".toRegex()) -> tokens.add(Token(matchResult.value, TokenType.HTML_TAG))
                matchResult.value.matches(Regex("\\b[a-zA-Z-]+(?=:)\\b")) -> tokens.add(Token(matchResult.value, TokenType.CSS_PROPERTY))
                matchResult.groups[4] != null -> tokens.add(Token(matchResult.value, TokenType.NUMBER))
                matchResult.groups[5] != null -> tokens.add(Token(matchResult.value, TokenType.FUNCTION))
                matchResult.value.matches("[=+\\-*/<>!&|%^~]+".toRegex()) -> tokens.add(Token(matchResult.value, TokenType.OPERATOR))
                matchResult.value.matches("\\b(TRUE|FALSE|null|this|super)\\b".toRegex()) -> tokens.add(Token(matchResult.value, TokenType.CONSTANT))
            }
            lastIndex = matchResult.range.last + 1
        }
        if (lastIndex < line.length) {
            tokens.add(Token(line.substring(lastIndex), TokenType.DEFAULT))
        }
        return tokens
    }
    private fun detectLanguage(code: String): String {
        val languagePatterns = mapOf(
            "Kotlin" to listOf("fun ", "val ", "data class", "companion object", "override fun"),
            "JavaScript" to listOf("function ", "=>", "console.log", "var ", "let ", "const "),
            "Python" to listOf("def ", "print(", "import ", "class ", "self", "elif"),
            "Java" to listOf("public class", "System.out.println", "void main", "import java", "@Override"),
            "C++" to listOf("#include", "std::", "using namespace", "cin >>", "cout <<"),
            "C" to listOf("#include", "printf(", "scanf(", "int main()", "return 0;"),
            "Bash" to listOf("#!/bin/bash", "if [", "then", "fi", "while", "do", "done", "case", "esac", "\\$\\{", "$\\HOME", "mkdir", "tar", "date"),
            "PHP" to listOf("<?php", "echo", "->", "\\$", "function", "array(", "class"),
            "HTML" to listOf("<!DOCTYPE html>", "<html>", "<head>", "<body>", "<div>", "<span>", "</html>"),
            "CSS" to listOf("{", "}", ":", ";", "color", "font-size", "margin"),
            "SQL" to listOf("SELECT ", "INSERT INTO", "CREATE TABLE", "WHERE", "UPDATE", "DELETE", "JOIN"),
            "Rust" to listOf("fn main()", "let mut", "::new", "println!", "use std::"),
            "Swift" to listOf("import Foundation", "func ", "let ", "var ", "class "),
            "Go" to listOf("package main", "import (", "fmt.Println", "func main()"),
            "Ruby" to listOf("def ", "puts ", "end", "class ", ":symbol"),
            "Perl" to listOf("#!/usr/bin/perl", "print ", "my $", "sub "),
            "Lua" to listOf("function ", "local ", "end", "then", "do"),
            "Haskell" to listOf("main :: IO", "do", "<-", "::", "->"),
            "R" to listOf("print(", "library(", "<-", "data.frame(", "ggplot("),
            "TypeScript" to listOf("import ", "const ", "let ", "interface", "type "),
            "Scala" to listOf("object ", "def ", "val ", "case class", "import scala"),
            "Assembly" to listOf("MOV ", "ADD ", "SUB ", "JMP ", "INT"),
            "Dart" to listOf("void main()", "import 'dart:", "class ", "@override", "print("),
            "Matlab" to listOf("function [", "disp(", "end", "plot(", "matrix"),
            "Objective-C" to listOf("#import ", "@interface", "@implementation", "NSLog(", "@property"),
            "VBScript" to listOf("Dim ", "Set ", "End Function", "MsgBox"),
            "JSON" to listOf("{", ":", "}", "[", "]", "true", "false"),
            "YAML" to listOf(":", "-", "key: value"),
            "Shell" to listOf("#!/bin/sh", "export ", "alias", "echo ", "source "),
            "Clojure" to listOf("(defn ", "(let [", "(println ", "{:key"),
            "Elixir" to listOf("defmodule ", "do: ", "IO.puts", "end", "%{}"),
            "Solidity" to listOf("pragma solidity", "contract ", "function ", "address", "msg.sender"),
            "JSON-Like" to listOf("{", "[", "]", "}", "\"key\": \"value\""),

        "Plain Text" to listOf("\n", " ")
        )
        for ((language, patterns) in languagePatterns) {
            if (patterns.any { code.contains(it) }) {
                return language
            }
        }
        return "Unknown Language 😭"
    }
    private fun getFontMetrics(font: Font): FontMetrics {
        val image = BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        graphics.font = font
        return graphics.fontMetrics
    }
    data class Token(val text: String, val type: TokenType)
    enum class TokenType {
        KEYWORD, STRING, COMMENT, NUMBER, FUNCTION, DEFAULT, OPERATOR, TYPE, VARIABLE, ANNOTATION, CONSTANT, BASH_COMMENT, HTML_TAG, CSS_PROPERTY, SQL_KEYWORD
    }
}
