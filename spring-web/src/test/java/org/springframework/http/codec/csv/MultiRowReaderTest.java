package org.springframework.http.codec.csv;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test for {@link MultiRowReader}.
 */
class MultiRowReaderTest {
    private final MultiRowReader reader = new MultiRowReader();

    /**
     * Test for {@link MultiRowReader#size()} and
     * implicitly for {@link MultiRowReader#addRow(String)}.
     */
    @Test
    void size() {
        assertThat(reader.size()).isZero();

        reader.addRow("abc");

        assertThat(reader.size()).isEqualTo(1);

        reader.addRow("abc");

        assertThat(reader.size()).isEqualTo(2);
    }

    /**
     * Test for {@link MultiRowReader#addRow(String)}.
     */
    @Test
    void add_empty() {
        reader.addRow("");

        assertThat(reader.size()).isZero();
    }

    /**
     * Test for {@link MultiRowReader#addRow(String)}.
     */
    @Test
    void add_first() {
        reader.addRow("ab");

        var destination1 = new char[1];
        var count1 = reader.read(destination1, 0, 1);
        assertThat(count1).isEqualTo(1);
        assertThat(destination1).containsExactly('a');
    }

    /**
     * Test for {@link MultiRowReader#addRow(String)}.
     * Test that adding a second row does not interrupt a prior read.
     */
    @Test
    void add_second() {
        reader.addRow("ab");

        var destination1 = new char[1];
        var count1 = reader.read(destination1, 0, 1);
        assertThat(count1).isEqualTo(1);
        assertThat(destination1).containsExactly('a');

        reader.addRow("cd");

        var destination2 = new char[1];
        var count2 = reader.read(destination2, 0, 1);
        assertThat(count2).isEqualTo(1);
        assertThat(destination2).containsExactly('b');

        var destination3 = new char[1];
        var count3 = reader.read(destination3, 0, 1);
        assertThat(count3).isEqualTo(1);
        assertThat(destination3).containsExactly('c');
    }

    /**
     * Test for {@link MultiRowReader#read(char[], int, int)}.
     * Test that an exception is thrown if the buffer is depleted unplanned.
     */
    @Test
    void read_empty() {
        assertThatThrownBy(() -> reader.read(new char[1], 0, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Test for {@link MultiRowReader#read(char[], int, int)} and
     * implicitly for {@link MultiRowReader#close()}.
     */
    @Test
    void read_end() {
        reader.close();

        assertThat(reader.read(new char[1], 0, 1)).isEqualTo(-1);
    }

    /**
     * Test for {@link MultiRowReader#read(char[], int, int)} and
     * implicitly for {@link MultiRowReader#addRow(String)}.
     */
    @Test
    void read() {
        reader.addRow("abc");
        reader.addRow("d");

        var destination1 = new char[2];
        var count1 = reader.read(destination1, 0, 2);
        assertThat(count1).isEqualTo(2);
        assertThat(destination1).containsExactly('a', 'b');

        var destination2 = new char[2];
        var count2 = reader.read(destination2, 0, 2);
        assertThat(count2).isEqualTo(1);
        assertThat(destination2).containsExactly('c', (char) 0);

        assertThatThrownBy(() -> reader.read(new char[1], 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
