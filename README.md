# Token Visualizer for Korean(nori) and Japanese(kuromoji)

- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Usage](#usage)
- [Examples](#examples)
- [References](#references)

It analyzes text using the Korean or Japanese text analyzer in Lucene and visualizes how the analyzers work. Both analyzers behave the same way by using a dictionary-based and finite-state-based approach to identify words in Korean or Japanese. 

When analyzing an input, it needs to perform a lookup in the dictionary on every character in order to build the lattice of all possible segmentations. In order to achieve this efficiently, we encode the full vocabulary in [FST (finite state transducer)](http://blog.mikemccandless.com/2010/12/using-finite-state-transducers-in.html). So we can analyze text using the Viterbi algorithm to find the most likely segmentation (called the Viterbi path) of any input written in Korean or Japanese. 

This token visualizer shows the Viterbi lattice built from input texts, for example, 1) "뿌리가 깊은 나무", 2) シンプルさは究極の洗練である, and 3) "세종시는 행정 수도" with a user dictionary. The Viterbi path in green shows the segmentation that nori/kuromoji outputs.

A node shows `<position (offset) of the input text>: <an id for Viterbi search>`.
An edge shows `<word> <cost of this word>`

1) "뿌리가 깊은 나무" 
```
$ java -jar TokenVisualizer.jar -t "뿌리가 깊은 나무"
```
![뿌리가깊은나무](https://user-images.githubusercontent.com/16463710/110339129-27a65400-806b-11eb-800e-6ba22e82021a.jpg)
```
뿌리 + 가 + 깊 + 은 + 나무
```
2) シンプルさは究極の洗練である
```
$ java -jar TokenVisualizer.jar -t "シンプルさは究極の洗練である"
```
![シンプルさは究極の洗練である](https://user-images.githubusercontent.com/16463710/110339172-312fbc00-806b-11eb-9c09-1fec9b9024d9.jpg)
```
シンプル + さ + は + 究極 + の + 洗練+ で + ある
```

3) "세종시는 행정 수도"
```
$ java -jar TokenVisualizer.jar -t "세종시는 행정 수도" -d user_dict.txt
```
in user_dict.txt
```
세종시 세종 시
```
![세종시는행정수도](https://user-images.githubusercontent.com/16463710/110339192-37259d00-806b-11eb-8e29-f08d55929560.jpg)
```
세종시 + 는 + 행정 + 수도
```

Usually, the algorithm to find the best segmentation uses three steps. It finds the next sentence boundary in the input first, then it builds the lattice of all possible paths for this sentence and finally it computes the best path (segmentation) by applying the cost of each transition. These steps are applied until all sentences in the input are consumed. Applying these steps sequentially can be slow, so nori/kuromoji uses a different approach. It uses a single step that is optimized for throughput. It consumes the input forward, character by character, and builds the Viterbi lattice on the fly. Costs are also computed on the fly in order to keep only the least costly path at each character boundary.

From the examples above, 1) the path `뿌 + 리가` is pruned as soon as we discover that `뿌리 + 가` has a lower cost because JKO (part of speech tag for `이`, `가`) has a lower cost after NNG according to morphological analysis, 2) the path `シ + ンプル` and `シン プル` is pruned as soon as we discover that `シンプル`has a lower cost, 3) the path `세종시` has -100000 cost because `세종시` is in the user dictionary and discovered as a compound noun (`세종 + 시`).

The processing stops at each frontier (when a state has only one possible transition) or after 1024 characters (to avoid memory issues if every state has multiple transitions) to output the best segmentation of the current lattice and restart the processing on the next character window.

Simply put, words in the dictionary have their own cost and the analyzer generates all possible ways using the dictionary to find the most likely segmentation of any input text.

## Prerequisites
- Graphviz from https://graphviz.org/download
- Java 1.8+

## Installation
Download `TokenVisualizer.jar` from https://github.com/uihyun/token-visualizer/releases/latest

For Windows, if you see the below error,
```
Cannot run program "dot.exe": error=2, No such file or directory
```
you need to add `<Graphviz installation path>/bin` to environment variables.
1. The default installation path is: `C:\Program Files (x86)\GraphvizX.XX\bin` (Example: GraphvizX.XX → Graphviz 2.46.1)
2. Go to Control Panel >  System and Security > System > Environment Variables
3. Select the entry Path in the system variables section and add `C:\Program Files (x86)\GraphvizX.XX\bin` to the existing path.

## Usage
```
$ java -jar TokenVisualizer.jar [Options]
```
```
Options:
    -h  help
    -o  output path (default: current directory)
    -m  decompound mode: discard|none|mixed for Korean (default: discard), search|normal|extended for Japanese (default: search)
    -m  language: ko for Korean, ja for Japapnese (default: ko)
    -t  "text" you want to analyze
    -d  user dictionary path
```

It will generate two files `.dot` for Graphviz and `.jpg` that includes a Viterbi lattice result.

## Examples
Example 1: `java -jar TokenVisualizer.jar -o /Users/elastic/Desktop/ -t "뿌리가 깊은 나무"`

Example 2: `java -jar TokenVisualizer.jar -o /Users/elastic/Desktop/ -m mixed -t "뿌리가 깊은 나무"`

Example 3: `java -jar TokenVisualizer.jar -o /Users/elastic/Desktop/ -t "세종시는 행정 수도" -d /Users/elastic/Desktop/user_dict.txt`

Example 4: `java -jar TokenVisualizer.jar -o /Users/elastic/Desktop/ -l ja -t "シンプルさは究極の洗練である"`

Example 5: `java -jar TokenVisualizer.jar -o /Users/elastic/Desktop/ -l ja -t "シンプルさは究極の洗練である" -d /Users/elastic/Desktop/user_dict.txt`

## References

- MeCab dictionary
  - https://en.wikipedia.org/wiki/MeCab
  - https://bitbucket.org/eunjeon/mecab-ko-dic
  - http://taku910.github.io/mecab/
- Finite-state machine
  - https://en.wikipedia.org/wiki/Finite-state_machine
- Viterbi algorithm
  - https://en.wikipedia.org/wiki/Viterbi_algorithm
- Elastic Blog posts
  - https://www.elastic.co/blog/nori-the-official-elasticsearch-plugin-for-korean-language-analysis
  - https://www.elastic.co/blog/how-to-implement-japanese-full-text-search-in-elasticsearch
