.PHONY: all clean

JC = javac
LIB = ./lib/
BIN = ./bin/
SRC = ./src/

CLASSPATH = $(SRC):$(LIB)*
JAVAFLAGS = -d $(BIN) -cp $(CLASSPATH)

ALLFILES = $(subst $(SRC), $(EMPTY), $(wildcard $(SRC)*.java))
CLASSFILES = $(ALLFILES:.java=.class)

all: $(addprefix $(BIN), $(CLASSFILES))

$(BIN)%.class: $(SRC)%.java
	$(JC) $(JAVAFLAGS) $<

clean:
	rm -r $(BIN)*
