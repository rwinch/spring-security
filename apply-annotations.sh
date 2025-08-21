#!/bin/bash
#
# Copyright (c) 2024 University of California, Riverside.
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
# THE SOFTWARE.
#

project="$1"
base_output_dir="$(pwd)/${project}/build"

# download annotator-core.jar from maven repository
#curl -O https://repo.maven.apache.org/maven2/edu/ucr/cs/riple/annotator/annotator-core/1.3.15/annotator-core-1.3.15.jar
cp /Users/rwinch/code/ucr-riple/NullAwayAnnotator/master/annotator-core/build/libs/annotator-core-1.3.16-SNAPSHOT.jar annotator-core-1.3.16-SNAPSHOT.jar

# make an EMPTY directory for the annotator output
annotator_out_dir="${base_output_dir}/annotator-out"
rm -rvf "$annotator_out_dir" && mkdir -p "$annotator_out_dir"

# make paths.tsv and add the placed nullaway_config_path and scanner_config_path
scanner_config_path="${annotator_out_dir}/scanner.xml"
nullaway_config_path="${annotator_out_dir}/nullaway.xml"
echo -e "$nullaway_config_path\t$scanner_config_path" > "${annotator_out_dir}/paths.tsv"

# run the annotator
java -jar annotator-core-1.3.16-SNAPSHOT.jar \
    -bc "cd $project  && gw compileJava --rerun-tasks" \
    -d "$annotator_out_dir" \
    -n org.jspecify.annotations.Nullable \
    -cp "${annotator_out_dir}/paths.tsv" \
    -cn NULLAWAY \
    -i com.uber.nullaway.annotations.Initializer \
    -sre org.jspecify.annotations.NullUnmarked
