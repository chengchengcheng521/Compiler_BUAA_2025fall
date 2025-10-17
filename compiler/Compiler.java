import error.Error;
import error.ErrorHandler;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import lexer.Lexer;
import lexer.Token;
import parser.Parser;

public class Compiler {
    public static void main(String[] args) {
        // 默认文件名
        String inputFile = "testfile.txt";
        String outputParserFile = "parser.txt";
        String outputErrorFile = "error.txt";

        // 如果提供了命令行参数，则使用它们
        if (args.length >= 1) {
            inputFile = args[0];
        }
        if (args.length >= 2) {
            outputParserFile = args[1];
        }
        if (args.length >= 3) {
            outputErrorFile = args[2];
        }

        // 控制是否输出 parser.txt 的开关, 用于评测。设为 true 即可输出。
        boolean enableParserOutput = true;

        try {
            // 1. 读取源文件
            // 先尝试UTF-8，如果失败则尝试GBK
            String source;
            try {
                source = Files.readString(Paths.get(inputFile), java.nio.charset.StandardCharsets.UTF_8);
            } catch (IOException e) {
                // UTF-8失败，尝试GBK
                source = Files.readString(Paths.get(inputFile), java.nio.charset.Charset.forName("GBK"));
            }

            // 2. 获取全局错误处理器实例
            ErrorHandler errorHandler = ErrorHandler.getInstance();

            // --- 阶段一：词法分析 ---
            Lexer lexer = new Lexer(source);
            lexer.run();

            // 词法分析有错时，后续的语法分析可能基于不完整的Token流，
            // 但根据作业要求，即使有错也要继续进行。

            List<Token> tokens = lexer.getTokens();

            // --- 阶段二：语法分析 ---
            PrintWriter parserWriter = null;
            if (enableParserOutput) {
                // 无论是否有错误，都进行语法分析并输出
                try {
                    parserWriter = new PrintWriter(outputParserFile);
                } catch (IOException e) {
                    System.err.println("创建 parser.txt 文件失败: " + e.getMessage());
                    enableParserOutput = false; // 无法写入，关闭输出功能
                }
            }

            Parser parser = new Parser(tokens, parserWriter);
            parser.parse(); // 启动语法分析

            // 关闭文件写入流（如果已打开）
            if (parserWriter != null) {
                parserWriter.close();
            }

            // --- 阶段三：错误处理与输出 ---
            if (errorHandler.hasErrors()) {
                // 如果在任何阶段（词法或语法）出现了错误，则统一输出到 error.txt
                writeErrors(errorHandler, outputErrorFile);
            } else {
                // 如果一切顺利，并且评测输出开关是打开的
                if (enableParserOutput) {
                    System.out.println("语法分析完成，结果已写入 " + outputParserFile);
                }
            }

        } catch (IOException e) {
            System.err.println("文件读写错误: " + e.getMessage());
        }
    }

    /**
     * 将错误处理器中的所有错误信息写入指定文件
     * @param handler 错误处理器实例
     * @param path    目标文件路径
     */
    private static void writeErrors(ErrorHandler handler, String path) {
        try (PrintWriter writer = new PrintWriter(path)) {
            // ErrorHandler 内部已经对错误按行号排序
            for (Error error : handler.getErrors()) {
                writer.println(error.toString());
            }
        } catch (IOException e) {
            System.err.println("写入错误文件失败: " + e.getMessage());
        }
        System.out.println("编译过程中发现错误，详情请见 " + path);
    }
}