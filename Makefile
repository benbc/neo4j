# -*- mode: makefile-gmake -*-
SHELL := bash

find = $(shell find $(1) -name "*.$(2)")
extract-tests = $(shell jar tf $(1) | grep 'Test.class$$' | sed -e 's|/|.|g;s|.class$$||')
ivy = java -jar build/ivy-2.4.0-rc1.jar -ivy $(1) -confs $(2) -cachepath $(3)

# org.neo4j:neo4j-primitive-collections:jar:2.2-SNAPSHOT
# org.neo4j:neo4j-io:jar:2.2-SNAPSHOT
# +- org.neo4j:neo4j-primitive-collections:jar:2.2-SNAPSHOT:compile
# org.neo4j:neo4j-csv:jar:2.2-SNAPSHOT
# org.neo4j:neo4j-kernel:jar:2.2-SNAPSHOT
# +- org.neo4j:neo4j-primitive-collections:jar:2.2-SNAPSHOT:compile
# +- org.neo4j:neo4j-io:jar:2.2-SNAPSHOT:compile
# +- org.neo4j:neo4j-csv:jar:2.2-SNAPSHOT:compile
# +- org.neo4j:neo4j-io:test-jar:tests:2.2-SNAPSHOT:test
# +- org.neo4j:neo4j-primitive-collections:test-jar:tests:2.2-SNAPSHOT:test

.DEFAULT_GOAL := test

name1 := primitive-collections
module1 := neo4j-${name1}
path1 := community/${name1}
main-jar1 := out/$(module1)-2.2-SNAPSHOT.jar
test-jar1 := out/$(module1)-2.2-SNAPSHOT-tests.jar
main-src1 := $(call find,$(path1)/src/main/java,java)
test-src1 := $(call find,$(path1)/src/test/java,java)

tmp/$(path1)/classpath: $(path1)/ivy.xml
	mkdir -p tmp/$(path1)
	$(call ivy,$<,compile,$@)

tmp/$(path1)/test-classpath: $(path1)/ivy.xml
	mkdir -p tmp/$(path1)
	$(call ivy,$<,test,$@)

$(main-jar1): tmp/$(path1)/classpath $(main-src1) | tmp out
	mkdir -p tmp/$(path1)/classes
	javac -d tmp/$(path1)/classes -classpath .:$$(cat tmp/$(path1)/classpath) $(main-src1)
	jar cf $@ -C tmp/$(path1)/classes .

$(test-jar1): tmp/$(path1)/test-classpath tmp/$(path1)/classpath $(main-jar1) $(test-src1) | tmp out
	mkdir -p tmp/$(path1)/test-classes
	javac -d tmp/$(path1)/test-classes \
		-classpath $$(cat tmp/$(path1)/classpath):$$(cat tmp/$(path1)/test-classpath):$(main-jar1) \
		$(test-src1)
	jar cf $@ -C tmp/$(path1)/test-classes .

out/$(module1)-tests-pass: $(test-jar1) $(jar) tmp/$(path1)/classpath tmp/$(path1)/test-classpath | out
	java -classpath $$(cat tmp/$(path1)/classpath):$$(cat tmp/$(path1)/test-classpath):$(main-jar1):$(test-jar1) \
		org.junit.runner.JUnitCore \
		$(call extract-tests,$(test-jar1))

name2 := io
module2 := neo4j-${name2}
path2 := community/${name2}
main-jar2 := out/$(module2)-2.2-SNAPSHOT.jar
test-jar2 := out/$(module2)-2.2-SNAPSHOT-tests.jar
main-src2 := $(call find,$(path2)/src/main/java,java)
test-src2 := $(call find,$(path2)/src/test/java,java)

tmp/$(path2)/classpath: $(path2)/ivy.xml
	mkdir -p tmp/$(path2)
	$(call ivy,$<,compile,$@)

tmp/$(path2)/test-classpath: $(path2)/ivy.xml
	mkdir -p tmp/$(path2)
	$(call ivy,$<,test,$@)

$(main-jar2): tmp/$(path2)/classpath $(main-src2) $(main-jar1) | tmp out
	mkdir -p tmp/$(path2)/classes
	javac -d tmp/$(path2)/classes -classpath .:$$(cat tmp/$(path2)/classpath):$(main-jar1) $(main-src2)
	jar cf $@ -C tmp/$(path2)/classes .

$(test-jar2): tmp/$(path2)/test-classpath tmp/$(path2)/classpath $(main-jar2) $(test-src2) $(main-jar1) | tmp out
	mkdir -p tmp/$(path2)/test-classes
	javac -d tmp/$(path2)/test-classes \
		-classpath $$(cat tmp/$(path2)/classpath):$$(cat tmp/$(path2)/test-classpath):$(main-jar2):$(main-jar1) \
		$(test-src2)
	jar cf $@ -C tmp/$(path2)/test-classes .

out/$(module2)-tests-pass: $(test-jar2) $(jar) tmp/$(path2)/classpath tmp/$(path2)/test-classpath $(main-jar1) | out
	java -classpath $$(cat tmp/$(path2)/classpath):$$(cat tmp/$(path2)/test-classpath):$(main-jar2):$(test-jar2):$(main-jar1) \
		org.junit.runner.JUnitCore \
		$(call extract-tests,$(test-jar2))

test: out/$(module1)-tests-pass out/$(module2)-tests-pass
.PHONY: test

out tmp:
	mkdir $@

clean:
	rm -rf out tmp
