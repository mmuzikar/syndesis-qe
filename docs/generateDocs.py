import sys
import os
import collections

#used to switch to various predefined ways the final table can look
"""
Default 'file' shows 
---
annotation | file name | line number
signature
---
compact
---
annotation
signature
---
ansig
---
annotation | signature 
---
"""
STYLE = "compact"

OUTPUTDIR = "docs/"

steps = []

all_steps = []

Annotation = collections.namedtuple("Annotation", ["filename", "start", "regex", "signature"])

def setup_filter():
    """
    Either creates the default filter to recognize the step definitions
    or parses it from the arguments provided
    """
    global annotation_filter
    if len(sys.argv) == 3:
        f = sys.argv[2]
        f = f.split(",")
        for a in f:
            annotation_filter.add(a)
    else: 
        annotation_filter = set(["when", "then", "given", "and", "but"])

def print_help():
    print("""
USAGE: generateDocs.py DIRECTORIES [FILTER]

Finds all cucumber steps in .java files in given directories annotated by given filter
The final documentation is generated to docs/ directory
Each \"module\" with at least one step defined is documented in its own file
and every step is documented in index.md 

Arguments:
\tDIRECTORIES\t comma separated list of directories to find definitions in.
\t\t (default value is the directory that the script is called from)
\tFILTER\t comma separated list of annotations that define steps
\t\t (default value is \"when,then,given,and,but\")

Example usage: 
    python3 generateDocs.py . "when,then"
        """)

def escape_regex(line):
    """
    Escapes the annotation line from the special characters used in markdown which could cause trouble
    """
    for c in "|>!#":
        line = line.replace(c, "\\" + c)
    return line

def process_lines(file_name, lines, i):
    """
    Tries to find annotation on line with index i,
    if an annotation is found then finds the function signature
    """
    annotation = lines[i].strip()
    index = annotation.find("(")
    #annotation was found
    if (index != -1):
        if annotation[1:index].lower() in annotation_filter:
            #regex = @give(...)
            annotation = escape_regex(annotation)
            #looking for signature
            signature = None
            signature_incomplete = True
            while signature_incomplete:
                i += 1
                line = lines[i].strip()
                if line.startswith("public"):
                    signature = line
                    signature_incomplete = not signature.endswith("{")
                elif signature:
                    #Fixes incomplete signatures when they are longer than one line
                    signature = signature + line
                    signature_incomplete = not signature.endswith("{")
            signature = signature.replace("{", "")
            level = 1
            starting_line_num = i
            i += 1
            steps.append(Annotation(file_name, starting_line_num, annotation, signature))
            return i
    return i

def scan_file(file_name):
    if not file_name.endswith(".java"):
        return
    try:
        with open(file_name, "r") as f:
            #Goes through all lines and tries to find annotations
            line_num = 0
            lines = f.readlines()
            while line_num < len(lines):
                line = lines[line_num].strip()
                if line.startswith("@"):
                    line_num = process_lines(file_name, lines, line_num)
                line_num += 1
    except Exception as e:
        print(e)

def create_index(paths):
    with open(OUTPUTDIR + "index.md", "w") as f:
        f.write("# Documentation of defined steps\n")
        for p in paths:
            #Links all other files to index
            name = p.replace("-", " ").capitalize()
            p = p + ".md"
            if os.path.exists(OUTPUTDIR + p):
                f.write("* [{}]({})\n".format(name, p))
        f.write("## All defined steps\n")
        write_table_header(f, all_steps)
        for s in all_steps:
            write_step_to_file(f, s)
    print("generated: index.md")

def write_table_header(f, steps):
    if STYLE == "file":
        f.write("Total defined steps: " + str(len(steps)) + "\n\n")
        f.write("| Definition | File name | Line number |\n")
        f.write("| --- | --- | --- |\n")
    elif STYLE == "ansig":
        f.write("Total defined steps: " + str(len(steps)) + "\n\n")
        f.write("| Definition | Signature |\n")
        f.write("| --- | --- |\n")
    elif STYLE == "compact":
        f.write("Total defined steps: " + str(len(steps)) + "\n\n")
        f.write("| Definition & Signature |\n")
        f.write("| --- |\n")

def write_step_to_file(f, s):
    if STYLE == "file":
        f.write(r"|`{regex}`<br>`{signature}`|[{file_name}]({file_name}#L{start})|{start}|"
            .format(regex=s.regex, signature=s.signature, file_name="../" + s.filename, start=s.start) + "\n")
    elif STYLE == "ansig":
        f.write(r"|[`{regex}`]({file_name}#L{start})|[`{signature}`]({file_name}#L{start})|"
            .format(regex=s.regex, signature=s.signature, file_name="../" + s.filename, start=s.start) + "\n")
    elif STYLE == "compact":
        f.write(r"|[`{annotation}`]({file_name}#L{start})<br>[`{signature}`]({file_name}#L{sigpos})|"
            .format(annotation=s.regex, signature=s.signature, file_name="../" + s.filename, start=s.start, sigpos=s.start+1) + "\n")

def main(path):
    if path == ".":
        #lists current directory
        path = ",".join(os.listdir("."))
    if "," in path:
        paths = path.split(",")
        for p in paths:
            #tries to find something to document in given path
            global steps
            steps = []
            #reseting of steps
            main(p)
        #creates index for all steps
        create_index(paths)
        return
    for dirName, dirs, files in os.walk(path):
        for f in files:
            scan_file(dirName + "/" + f)   

    #changes path not to screw up any paths nor file types
    sanitized_path = path.replace(".", "").replace("/", "")
    if len(steps) > 0:
        with open(OUTPUTDIR + sanitized_path + ".md", "w") as f:
            f.write("# Step definitions of " + sanitized_path + "\n")
            write_table_header(f, steps)
            s = steps.pop()
            while s:
                write_step_to_file(f, s)
                #appends step to global list of all defined steps to later create index
                all_steps.append(s)
                if len(steps) > 0:
                    s = steps.pop()
                else:
                    break
        print("generated file: " + sanitized_path + ".md")

if __name__ == "__main__":
    setup_filter()
    if len(sys.argv) < 2:
        print_help()
    else:
        main(sys.argv[1])
