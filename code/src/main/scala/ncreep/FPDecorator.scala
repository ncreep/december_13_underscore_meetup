package ncreep

object FPDecorator {
  case class Programmer(experience: XPPoints,
                        coffeeConsumption: Coffee,
                        languages: Set[Language])

  type ForProgrammer[A] = Programmer => A

  val makeCode: Programmer => (Coffee => LOC) =
    p =>
      c => p.experience * math.max(0, c - p.coffeeConsumption)

  val price: Programmer => Money =
    p => p.experience * p.languages.size

  val acceptProject: Programmer => (Language => Boolean) =
    p =>
      l => p.languages(l)

  val testMakeCode: (Coffee => LOC) => (Coffee => LOC) =
    mc =>
      c => mc(c - 5) * 3

  val testPrice: Money => Money =
    m => m * 2

  val docPrice: Money => Money =
    m => m * 10

  val saneAccept: (Language => Boolean) => (Language => Boolean) =
    ap =>
      l => if (l == "PHP") false else ap(l)

  val polyAccept: (Language => Boolean) => (Language => Boolean) =
    ap =>
      l => !ap(l)

  val shouldHire: Money => Boolean = m => m < 50
  val isProductive: LOC => (Programmer => Boolean) = loc => p => loc > p.coffeeConsumption

  val bonus: Money => (Programmer => Money) =
    m => p => m + p.experience

  val deadProgrammer: (Coffee => LOC) => Programmer => (Coffee => LOC) =
    mc => p =>
      c => if (c > 4 * p.coffeeConsumption) mc(c) else 0

  implicit class Decorator[A](f: Programmer => A) {
    def flatAndThen[B](g: A => Programmer => B): Programmer => B =
      p => {
        val fg: Programmer => (Programmer => B) = (f andThen g)
        val toB: Programmer => B = fg(p)
        val b: B = toB(p)
        b
      }
    
    def flatMap[B](g: A => Programmer => B): Programmer => B =
      p => g(f(p))(p)

    def map[B](g: A => B): Programmer => B = f andThen g
    def >>[B](g: A => B): Programmer => B = f andThen g
  }

  val getProgrammer: Programmer => Programmer = p => p

  def main(args: Array[String]): Unit = {
    val p = Programmer(
        experience = 10, 
        coffeeConsumption = 30, 
        languages = Set("Scala", "Java", "PHP"))

    val testMC = makeCode andThen testMakeCode
    val testDocPrice = price andThen testPrice andThen docPrice
    val testDocPrice2 = for {
      m <- price
    } yield m * 2
    val saneAcc = acceptProject andThen saneAccept

    val docPr = price andThen docPrice
    val polyAcc = acceptProject andThen polyAccept

    price(p) // 30
    makeCode(p)(40) // 100
    acceptProject(p)("PHP") // true

    testDocPrice(p) // 600
    testMC(p)(40) // 150
    saneAcc(p)("PHP") // false

    docPr(p) // 300
    polyAcc(p)("PHP") // false

    val executiveDecision1: Programmer => Boolean =
      price andThen shouldHire
    val executiveDecision2: Programmer => Boolean =
      price andThen testPrice andThen shouldHire
    executiveDecision1(p) // true
    executiveDecision2(p) // false

    val dead2 = for {
      mc <- makeCode
      p1 <- getProgrammer
    } yield (c: Coffee) => if (c > 4 * p1.coffeeConsumption) mc(c) else 0

    val prod = for {
      mc <- makeCode
      p <- getProgrammer
      loc = mc(50)
    } yield loc > p.coffeeConsumption

    val prod2 = makeCode andThen (mc => mc(50)) flatAndThen isProductive

    val deadMake: Programmer => (Coffee => LOC) = 
      makeCode flatAndThen deadProgrammer andThen testMakeCode
      
//    println(deadMake(p)(130))
//    println(deadMake(p)(50))
    val dead3 = makeCode flatAndThen (mc => (p1 => (c: Coffee) => if (c > 4 * p1.coffeeConsumption) mc(c) else 0))

    val costEffective = for {
      mc <- makeCode
      p <- price
    } yield mc andThen (loc => loc > p)

    val costEffective2: Programmer => (Coffee => Boolean) =
      makeCode flatAndThen (mc =>
        price andThen (pr =>
          mc andThen (loc => loc > pr)
        ))
    
    costEffective2(p)(30) // false
    costEffective2(p)(120) // true

    val withBonus: Programmer => Money = price andThen testPrice flatAndThen bonus
    withBonus(p)

    val x = List(
      price(p),
      makeCode(p)(40),
      acceptProject(p)("PHP"),

      testDocPrice(p),
      testMC(p)(40),
      saneAcc(p)("PHP"),

      docPr(p),
      makeCode(p)(40),
      polyAcc(p)("PHP")
    )
//    x.foreach(println)
  }

}