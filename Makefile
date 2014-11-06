# -*- mode: makefile-gmake -*-
SHELL := bash

find = $(shell find $(1) -name "*.$(2)")
extract-tests = $(shell jar tf $(1) | grep 'Test.class$$' | sed -e 's|/|.|g;s|.class$$||')
ivy = java -jar build/ivy-2.4.0-rc1.jar -ivy $(1) -confs $(2) -cachepath $(3)

module := neo4j-primitive-collections
path := community/primitive-collections
main-jar := out/$(module)-2.2-SNAPSHOT.jar
test-jar := out/$(module)-2.2-SNAPSHOT-tests.jar
main-src := $(call find,$(path)/src/main/java,java)
test-src := $(call find,$(path)/src/test/java,java)

test: out/$(module)-tests-pass
.PHONY: test

tmp/$(path)/classpath: $(path)/ivy.xml
	mkdir -p tmp/$(path)
	$(call ivy,$<,compile,$@)

tmp/$(path)/test-classpath: $(path)/ivy.xml
	mkdir -p tmp/$(path)
	$(call ivy,$<,test,$@)

$(main-jar): tmp/$(path)/classpath $(main-src) | tmp out
	mkdir -p tmp/$(path)/classes
	javac -d tmp/$(path)/classes -classpath .:$$(cat tmp/$(path)/classpath) $(main-src)
	jar cf $@ -C tmp/$(path)/classes .

$(test-jar): tmp/$(path)/test-classpath tmp/$(path)/classpath $(main-jar) $(test-src) | tmp out
	mkdir -p tmp/$(path)/test-classes
	javac -d tmp/$(path)/test-classes \
		-classpath $$(cat tmp/$(path)/classpath):$$(cat tmp/$(path)/test-classpath):$(main-jar) \
		$(test-src)
	jar cf $@ -C tmp/$(path)/test-classes .

out/$(module)-tests-pass: $(test-jar) $(jar) tmp/$(path)/classpath tmp/$(path)/test-classpath | out
	java -classpath $$(cat tmp/$(path)/classpath):$$(cat tmp/$(path)/test-classpath):$(main-jar):$(test-jar) \
		org.junit.runner.JUnitCore \
		$(call extract-tests,$(test-jar))

out tmp:
	mkdir $@

clean:
	rm -rf out tmp
