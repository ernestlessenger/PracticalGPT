import os
import re
import argparse
import markdown
from langchain.chat_models import ChatOpenAI
from langchain.prompts.chat import (
    ChatPromptTemplate,
    SystemMessagePromptTemplate,
    AIMessagePromptTemplate,
    HumanMessagePromptTemplate,
)
from langchain.schema import AIMessage, HumanMessage, SystemMessage
from langchain.document_loaders import PyPDFLoader
import pypdf

def extract(input_pdf):
    loader = PyPDFLoader(input_pdf)
    pages = loader.load()

    text = ''
    for page in pages:
        text += '\n' + page.page_content
    return text

def to_file(text, filename, output_folder):
    with open(os.path.join(output_folder, filename + ".md"), "w") as file:
        file.write(text)
    html = markdown.markdown(text)
    with open(os.path.join(output_folder, filename + ".html"), "w") as file:
        file.write(html)

def prompt1(resume):
    prompt = f"""You are a recruiter for a Fortune-500 IT department.
Reformat this resume to be easy to read.
Use only information provided in this resume. Do not add information or use information from other sources.
Include four sections: A two-paragraph summary; Education; Experience; Familiar Technologies
Format your response as Markdown with the first header at level 1. Enclose your response in <response></response> tags.
<resume>
{resume}
</resume>"""
    return prompt

def prompt2(resume):
    prompt = f"""You are a recruiter for a Fortune-500 IT department.
Write a 1000 word cover letter explaining why this candidate is the perfect person for the job described below.
This job does not yet exist, so you will need to also explain why this job position would benefit Peace Health.
Format your response as Markdown with the first header at level 1. Enclose your response in <response></response> tags.

<job>Peace Health in Eugene, Oregon is looking for the right person to manage their Artificial Intelligence practice. The ideal candidate will have recent experience working with Generative AI, Machine Learning, and Data Science.</job>

<resume>
{resume}
</resume>"""
    return prompt

def complete(prompt, apikey):
    openai = ChatOpenAI(model_name="gpt-4", openai_api_key=apikey)

    prompt_msgs = [
        HumanMessage(
            content=prompt
        ),
        HumanMessage(content="Tips: Make sure to answer in the correct format"),
    ]

    response = openai(prompt_msgs)
    return response.content

def get_text_between_tags(source, tag):
    start_tag = f"<{tag}>"
    end_tag = f"</{tag}>"
    pattern = re.compile(f"{start_tag}(.*?){end_tag}", re.DOTALL | re.IGNORECASE | re.MULTILINE)
    matcher = pattern.search(source)
    return matcher.group(1).strip() if matcher else source

def main():
    parser = argparse.ArgumentParser(description='Converts a PDF resume into other formats')
    parser.add_argument('--input', help='The file (PDF) to be converted')
    parser.add_argument('--output', help='The output folder')
    parser.add_argument('--apikey', help='OpenAI API Key')
    args = parser.parse_args()

    output_folder = args.output

    print("Extracting resume")
    text = extract(args.input)
    to_file(text, "resume_raw", output_folder)

    print("Generating formatted resume")
    resume_prompt = prompt1(text)
    to_file(resume_prompt, "resume_prompt", output_folder)

    resume_response = complete(resume_prompt, args.apikey)
    to_file(resume_response, "resume", output_folder)

    print("Generating cover letter")
    cl_prompt = prompt2(resume_response)
    to_file(cl_prompt, "cover_letter_prompt", output_folder)

    cl = complete(cl_prompt, args.apikey)
    to_file(cl, "cover_letter", output_folder)


if __name__ == "__main__":
    main()