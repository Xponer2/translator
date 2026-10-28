package io.github.xponer2.translator.text;

import net.minecraft.util.text.TextComponentString;

/**
 * 1.21 {@code Component.literal(text)}.
 */
public class LiteralComponent extends TextComponentString implements MutableComponent {
	public LiteralComponent(String text) {
		super(text);
	}

	@Override
	public LiteralComponent createCopy() {
		var copy = new LiteralComponent(this.getText());
		copy.setStyle(this.getStyle().createShallowCopy());
		for (var sibling : this.getSiblings()) {
			copy.appendSibling(sibling.createCopy());
		}

		return copy;
	}
}
