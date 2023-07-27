# Practical GPT for Business

This is the source repository for the examples from Practical GPT for Business by Ernest W. Lessenger.

- GPT4Service - A simple Java service that uses the OpenAI API to generate a response to a prompt.
- Main - An executable java class that uses iText-core to extract text from a PDF file and then uses the OpenAI API to reformat the resume and create a cover letter.

## Java Usage

- Create a file called resume.pdf in the same directory as the jar file.
- Run the jar file with the following commands:

```bash
mvn package
java -cp ./target/PracticalGPT.jar com.gpt.practicalgpt.Main --input resume.pdf --output ./ --apikey <your api key>
```

## Python Usage

- Create a file called resume.pdf in the same directory as the Main.py file.
- Run the Main.py file with the following commands:

```bash
pip install -r requirements.txt
python3 Main.py --input data/resume.pdf --output ./data/ --apikey <your api key>
```

# License

This project is licensed under the MIT License - see the [mit.md](mit.md) file for details

This project uses the iText-core library, which is licensed under the AGPL license. See [https://www.gnu.org/licenses/agpl-3.0.en.html](https://www.gnu.org/licenses/agpl-3.0.en.html) for details.