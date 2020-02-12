TARGET=publishing.dot.png
SRC=publishing.dot

$(TARGET): $(SRC)
	dot -Tpng $(SRC) -O

.PHONY: clean
clean:
	$(RM) $(TARGET)
