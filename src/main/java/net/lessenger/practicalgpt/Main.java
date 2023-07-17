package net.lessenger.practicalgpt;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import lombok.extern.slf4j.Slf4j;
import org.commonmark.node.Node;
import picocli.CommandLine;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@CommandLine.Command(name = "convert", mixinStandardHelpOptions = true, version = "convert 1.0", description = "Converts a PDF resume into other formats")
class Main implements Callable<Integer> {

    @CommandLine.Option(names = {"--input"}, description = "The file to be converted")
    private File inputFile;
    @CommandLine.Option(names = {"--output"}, description = "The output folder")
    private File output;
    @CommandLine.Option(names = {"--apikey"}, description = "OpenAI API Key")
    private String apikey;

    private Path outputFolder;

    @Override
    public Integer call() throws Exception { // your business logic goes here...
        outputFolder = output.toPath();

        // Extract the raw PDF text
        final String text = extract(inputFile);

        // Write the raw text to file
        toFile(text, "resume_raw");

        // Create the prompt
        final String resume_prompt = prompt1(text);
        toFile(resume_prompt, "resume_prompt");

        // Get the formatted resume from ChatGPT
        final String resume_response = complete(resume_prompt);
        toFile(resume_response, "resume");

        // Create a new prompt for the cover letter, and get the cover letter
        final String cl_prompt = prompt2(resume_response);
        toFile(cl_prompt, "cover_letter_prompt");
        final String cl = complete(cl_prompt);
        toFile(cl, "cover_letter");

        return 0;
    }

    /**
     * Writes this text to file
     * @param text
     * @return
     */
    private void toFile(final String text, final String filename) throws IOException {

        // Write the Markdown response to file
        final FileWriter mdWriter = new FileWriter(outputFolder.resolve(filename + ".md").toFile());
        mdWriter.write(text);
        mdWriter.close();

        // Write the HTML to file
        final String html = Markdown2HTML(text);

        final FileWriter htmlWriter = new FileWriter(outputFolder.resolve(filename + ".html").toFile());
        htmlWriter.write(html);
        htmlWriter.close();
    }

    /**
     * Use iText to extract the text from the provided PDF
     *
     * @return The text of the PDF
     */
    private String extract(final File inputPDF) throws IOException {
        final PdfReader reader = new PdfReader(inputPDF);
        final PdfDocument document = new PdfDocument(reader);

        // Read page by page
        final StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= document.getNumberOfPages(); i++) {
            sb.append(PdfTextExtractor.getTextFromPage(document.getPage(i)));
            sb.append("\n");
        }

        document.close();
        reader.close();

        return sb.toString();
    }

    private String prompt1(final String resume) {
        final StringBuilder sb = new StringBuilder();

        sb.append("You are a recruiter for a Fortune-500 IT department.\n");
        sb.append("Reformat this resume to be easy to read.\n");
        sb.append("Use only information provided in this resume. Do not add information or use information from other sources.\n");
        sb.append("Include four sections: A two-paragraph summary; Education; Experience; Familiar Technologies\n");
        sb.append("Format your response as Markdown with the first header at level 1. Enclose your response in <response></response> tags.\n");
        sb.append("<resume>\n");
        sb.append(resume);
        sb.append("\n");
        sb.append("</resume>");

        return sb.toString();
    }

    private String prompt2(final String resume) {
        final StringBuilder sb = new StringBuilder();

        sb.append("You are a recruiter for a Fortune-500 IT department.\n");
        sb.append("Write a 1000 word cover letter explaining why this candidate is the perfect person for the job described below.\n");
        sb.append("This job does not yet exist, so you will need to also explain why this job position would benefit Peace Health.\n");
        sb.append("Format your response as Markdown with the first header at level 1. Enclose your response in <response></response> tags.\n\n");
        sb.append("<job>Peace Health in Eugene, Oregon is looking for the right person to manage their Artificial Intelligence practice. The ideal candidate will have recent experience working with Generative AI, Machine Learning, and Data Science.</job>\n\n");
        sb.append("<resume>\n");
        sb.append(resume);
        sb.append("\n");
        sb.append("</resume>");

        return sb.toString();
    }

    private String complete(final String prompt) {
        final GPT4Service gpt4Service = new GPT4Service();
        gpt4Service.setApiKey(apikey);

        try {
            return getTextBetweenTags(gpt4Service.complete(prompt), "response");
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Converts the provided Markdown to HTML using the CommonMark library
     * @param md
     * @return
     */
    private String Markdown2HTML(final String md) {
        org.commonmark.renderer.Renderer renderer = org.commonmark.renderer.html.HtmlRenderer.builder().build();
        Node node = org.commonmark.parser.Parser.builder().build().parse(md);
        return renderer.render(node);
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }



    /**
     * Uses regular expressions to extract the content between two tags
     *
     * @param source The source to search
     * @param tag    The tag name, excluding brackets, to use as the enclosure
     * @return The text between the two tags
     */
    protected static String getTextBetweenTags(final String source, final String tag) {
        String startTag = "<" + tag + ">";
        String endTag = "</" + tag + ">";

        final Pattern pattern = Pattern.compile(startTag + "(.*?)" + endTag, Pattern.DOTALL + Pattern.CASE_INSENSITIVE + Pattern.MULTILINE);
        final Matcher matcher = pattern.matcher(source);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return source;
    }


}