# Token Visualizer for Korean(nori) and Japanese(kuromoji)

- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Usage](#usage)
- [Examples](#examples)
- [References](#references)

It analyzes text using the Korean or Japanese text analyzer in Lucene and visualizes how the analyzers work. Both analyzers behave the same way by using a dictionary-based and finite-state-based approach to identify words (aka tokens) in Korean or Japanese. 

When analyzing an input, it needs to perform a lookup in the dictionary on every character in order to build the lattice of all possible segmentations. In order to achieve this efficiently, we encode the full vocabulary in [FST (finite state transducer)](http://blog.mikemccandless.com/2010/12/using-finite-state-transducers-in.html). So we can analyze text using the Viterbi algorithm to find the most likely segmentation (called the Viterbi path) of any input written in Korean or Japanese. 

This token visualizer shows the Viterbi lattice built from input texts, for example, 1) "뿌리가 깊은 나무", 2) シンプルさは究極の洗練である, and 3) "세종시는 행정 수도" with a user dictionary. The Viterbi path in green shows the segmentation that nori/kuromoji outputs.

In this graph,
- Node has `<position(offset) of the input text>: <id for Viterbi search>`.
- Edge has `<token> <token's cost> <token's part of speech>`.

#### 1) "뿌리가 깊은 나무" 
```
$ java -jar TokenVisualizer.jar -t "뿌리가 깊은 나무"
```
![뿌리가깊은나무 with PoS](https://user-images.githubusercontent.com/16463710/117391266-8fbdcd00-af2a-11eb-8eb5-0fe354bab263.jpg)

```
뿌리(NNG) + 가(J) + 깊(VA) + 은(E) + 나무(NNG)
```
- The path `뿌 + 리가` is pruned as soon as we discover that `뿌리 + 가` has a lower cost because J(Ending Particle aka 조사, part of speech tag for `이`, `가`) has a lower cost after NNG(General Noun) according to morphological analysis


#### 2) シンプルさは究極の洗練である
```
$ java -jar TokenVisualizer.jar -l ja -t "シンプルさは究極の洗練である"
```
![シンプルさは究極の洗練である with PoS](https://user-images.githubusercontent.com/16463710/117389007-a3673480-af26-11eb-9079-b04a55297372.jpg)

```
シンプル(名詞-形容動詞語幹) + さ(名詞-接尾-特殊) + は(助詞-係助詞) + 究極(名詞-一般) + の(助詞-連体化) + 洗練(名詞-サ変接続) + で(助動詞) + ある(助動詞)
```
- The path `シ + ンプル` and `シン プル` is pruned as soon as we discover that `シンプル` has a lower cost as 名詞-形容動詞語幹.
- The path `究極 + の` has a lower cost when `の`(助詞-連体化) comes next to `究極`(名詞-一般) than other combirations such as `究 + 極 + の`.
 

#### 3) "세종시는 행정 수도"
```
$ java -jar TokenVisualizer.jar -t "세종시는 행정 수도" -d user_dict.txt
```
in user_dict.txt
```
세종시 세종 시
```
![세종시는행정수도 with PoS](https://user-images.githubusercontent.com/16463710/117391161-58e7b700-af2a-11eb-8c07-fae14c4cc793.jpg)

```
세종시(NNG) + 는(J) + 행정(NNG) + 수도(NNG)
```
- The path `세종시` has -100000 cost because `세종시` is in the user dictionary and discovered as a compound noun (`세종 + 시` NNG).


Usually, the algorithm to find the best segmentation uses three steps. It finds the next sentence boundary in the input first, then it builds the lattice of all possible paths for this sentence and finally it computes the best path (segmentation) by applying the cost of each transition. These steps are applied until all sentences in the input are consumed. Applying these steps sequentially can be slow, so nori/kuromoji uses a different approach. It uses a single step that is optimized for throughput. It consumes the input forward, character by character, and builds the Viterbi lattice on the fly. Costs are also computed on the fly in order to keep only the least costly path at each character boundary.

Let's see the mentions below examples again to understand,
1) the path `뿌 + 리가` is pruned as soon as we discover that `뿌리 + 가` has a lower cost because J(Ending Particle aka 조사, part of speech tag for `이`, `가`) has a lower cost after NNG(General Noun) according to morphological analysis
2) the path `シ + ンプル` and `シン プル` is pruned as soon as we discover that `シンプル` has a lower cost as 名詞-形容動詞語幹.
3) the path `究極 + の` has a lower cost when `の`(助詞-連体化) comes next to `究極`(名詞-一般) than other combirations such as `究 + 極 + の`.
4) the path `세종시` has -100000 cost because `세종시` is in the user dictionary and discovered as a compound noun (`세종 + 시` NNG).

More details about part of speech:
- [Korean 품사](https://docs.google.com/spreadsheets/d/1-9blXKjtjeKZqsf4NzHeYJCrr49-nXeRF6D80udfcwY/edit#gid=589544265)
- [Japanese 品詞](http://taku910.github.io/mecab/posid.html)

The processing stops at each frontier (when a state has only one possible transition) or after 1024 characters (to avoid memory issues if every state has multiple transitions) to output the best segmentation of the current lattice and restart the processing on the next character window.

Simply put, words in the dictionary have their own cost and the analyzer generates all possible ways using the dictionary to find the most likely segmentation of any input text.

## Prerequisites

### Required
- **Java 8 or higher** - Required to run the application
- **Graphviz** - Required for graph visualization
  - Download from https://graphviz.org/download
  - Make sure `dot` command is available in your system PATH

### For Building (Optional)
- **Maven 3.x** - Only needed if building from source
- **Git** - Only needed if cloning the repository

### System Requirements
- **Memory**: 512MB RAM minimum (more recommended for large texts)
- **Disk Space**: 50MB for the application
- **Network**: Not required (runs completely offline)

## Installation

### Option 1: Download Pre-built JAR
Download `TokenVisualizer.jar` from https://github.com/uihyun/token-visualizer/releases/latest

### Option 2: Build from Source
If you want to build from source code:

```bash
# Clone the repository
git clone https://github.com/uihyun/token-visualizer.git
cd token-visualizer

# Build with Maven
mvn clean package

# The executable JAR will be created at:
# target/TokenVisualizer.jar
```

**Build Requirements**: Java 8+ and Maven 3.x

**Build Output**:
- Main JAR: `target/TokenVisualizer.jar` (includes all dependencies)
- Original JAR: `target/token-visualizer-1.0.0.jar` (without dependencies)

**Build Time**: Usually takes 2-3 minutes depending on your internet connection

### Windows Setup
For Windows, if you see the below error,
```
Cannot run program "dot.exe": error=2, No such file or directory
```
you need to add `<Graphviz installation path>/bin` to environment variables.
1. The default installation path is: `C:\Program Files (x86)\GraphvizX.XX\bin` (Example: GraphvizX.XX → Graphviz 2.46.1)
2. Go to Control Panel >  System and Security > System > Environment Variables
3. Select the entry Path in the system variables section and add `C:\Program Files (x86)\GraphvizX.XX\bin` to the existing path.

## Usage

Token Visualizer now supports two modes: **Web UI mode** and **CLI mode**.

### Web UI Mode
Simply run the JAR without any arguments to start the web server:
```bash
java -jar TokenVisualizer.jar
# Open your browser and go to http://localhost:8088
```

The web interface provides:
- Interactive text input with real-time visualization
- Language selection (Korean/Japanese)
- Analysis mode selection
- User dictionary file upload
- Instant visualization results in your browser

To use a different port:
```bash
java -jar TokenVisualizer.jar --server.port=9090
```

<img width="2064" height="1628" alt="image" src="https://github.com/user-attachments/assets/1279e796-911a-4977-ba70-16a3e775a9d7" />

### CLI Mode (Traditional)
Run with command-line arguments for the traditional CLI interface:

If you downloaded the pre-built JAR:
```
$ java -jar TokenVisualizer.jar [Options]
```

If you built from source:
```
$ java -jar target/TokenVisualizer.jar [Options]
```
```
Options:
    -h  help
    -o  output path (default: current directory)
    -m  decompound mode: discard|none|mixed for Korean (default: discard), search|normal|extended for Japanese (default: search)
    -l  language: ko for Korean, ja for Japapnese (default: ko)
    -t  "text" you want to analyze
    -d  user dictionary path
```

CLI mode will generate two files `.dot` for Graphviz and `.jpg` that includes a Viterbi lattice result.

## Examples

### Using Pre-built JAR
Example 1: `java -jar TokenVisualizer.jar -t "뿌리가 깊은 나무"`

Example 2: `java -jar TokenVisualizer.jar -o /Users/elastic/Desktop/ -m mixed -t "뿌리가 깊은 나무"`

Example 3: `java -jar TokenVisualizer.jar -o /Users/elastic/Desktop/ -t "세종시는 행정 수도" -d /Users/elastic/Desktop/user_dict.txt`

Example 4: `java -jar TokenVisualizer.jar -l ja -t "シンプルさは究極の洗練である"`

Example 5: `java -jar TokenVisualizer.jar -o /Users/elastic/Desktop/ -l ja -t "シンプルさは究極の洗練である" -d /Users/elastic/Desktop/user_dict.txt`

### Using Built from Source
If you built the project locally, replace `TokenVisualizer.jar` with `target/TokenVisualizer.jar` in all the above examples.

Example: `java -jar target/TokenVisualizer.jar -t "뿌리가 깊은 나무"`

### Web UI Mode Examples
Start the web server:
```bash
# Default port (8088)
java -jar TokenVisualizer.jar

# Custom port
java -jar TokenVisualizer.jar --server.port=8090
```

Then open your browser and navigate to the specified URL to use the interactive web interface.

### Web UI Features

The web interface provides a modern, user-friendly way to analyze tokens:

1. **Interactive Text Input**
   - Enter Korean or Japanese text in the textarea
   - Real-time language detection and mode suggestions
   - Support for long text analysis

2. **Language and Mode Selection**
   - Choose between Korean (한국어) and Japanese (日本語)
   - Mode options automatically update based on selected language:
     - **Korean modes**: Discard (default), None, Mixed
     - **Japanese modes**: Search (default), Normal, Extended

3. **User Dictionary Upload**
   - Optional user dictionary support via file upload
   - Simply drag and drop or click to upload `.txt` files
   - Custom tokenization rules for domain-specific terms

4. **Advanced Visualization**
   - Results display as interactive images
   - **Click the image** or **"Open Full View"** button to open detailed view in new tab
   - Full-screen analysis with zoom and pan capabilities

5. **Full View Mode** (opens in new tab)
   - **Zoom Controls**: Zoom In (+), Zoom Out (-), Reset (100%), Fit to Window
   - **Mouse Controls**: 
     - Ctrl/Cmd + Mouse Wheel: Zoom in/out
     - Click and Drag: Pan around large graphs
   - **Keyboard Shortcuts**: `+`, `-`, `0` (reset), `F` (fit to window)
   - **Automatic Fitting**: Initially fits the visualization to window size

6. **Responsive Design**
   - Works on desktop and mobile devices
   - Optimized for various screen sizes
   - Modern, clean interface

### How to Use Web UI

1. **Start the application**:
   ```bash
   java -jar TokenVisualizer.jar
   ```

2. **Open your browser** and go to `http://localhost:8088`

3. **Enter your text** in the text area (Korean or Japanese)

4. **Select language and mode** (optional - defaults are usually good)

5. **Upload user dictionary** (optional - for custom terms)

6. **Click "Visualize Tokens"** to generate the analysis

7. **View results**:
   - Small preview shows in the main page
   - Click the image or "Open Full View" for detailed analysis
   - Use zoom and pan controls in the full view for complex graphs

## References

- MeCab dictionary
  - https://en.wikipedia.org/wiki/MeCab
  - https://bitbucket.org/eunjeon/mecab-ko-dic
  - http://taku910.github.io/mecab/
- Finite-state machine
  - https://en.wikipedia.org/wiki/Finite-state_machine
- Viterbi algorithm
  - https://en.wikipedia.org/wiki/Viterbi_algorithm
- Elastic documentation
  - https://www.elastic.co/guide/en/elasticsearch/plugins/current/analysis-nori.html
  - https://www.elastic.co/guide/en/elasticsearch/plugins/current/analysis-kuromoji.html
  - https://www.elastic.co/blog/nori-the-official-elasticsearch-plugin-for-korean-language-analysis
  - https://www.elastic.co/blog/how-to-implement-japanese-full-text-search-in-elasticsearch
